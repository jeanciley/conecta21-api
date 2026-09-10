package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AnexoRespostaDTO;
import br.com.conecta21.api.model.AnexoChamado;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.repository.AnexoChamadoRepository;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AnexoChamadoService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "txt", "log", "pdf"
    );

    private final AnexoChamadoRepository anexoRepository;
    private final ChamadoRepository chamadoRepository;
    private final TenantContext tenantContext;
    private final Path diretorioRaiz;

    public AnexoChamadoService(
            AnexoChamadoRepository anexoRepository,
            ChamadoRepository chamadoRepository,
            TenantContext tenantContext,
            @Value("${app.arquivos.diretorio:uploads/chamados}") String diretorioRaiz) {
        this.anexoRepository = anexoRepository;
        this.chamadoRepository = chamadoRepository;
        this.tenantContext = tenantContext;
        this.diretorioRaiz = Path.of(diretorioRaiz).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepararDiretorio() {
        try {
            Files.createDirectories(diretorioRaiz);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível preparar o diretório de anexos.", ex);
        }
    }

    public void salvarAnexos(Chamado chamado, List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            return;
        }

        for (MultipartFile arquivo : arquivos) {
            if (arquivo == null || arquivo.isEmpty()) {
                continue;
            }
            salvar(chamado, arquivo);
        }
    }

    public List<AnexoRespostaDTO> listar(Long chamadoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        garantirChamadoNoTenant(chamadoId, empresaId);

        return anexoRepository
                .findAllByChamadoIdAndChamadoEmpresaIdOrderByDataUploadAsc(chamadoId, empresaId)
                .stream()
                .map(this::toResposta)
                .toList();
    }

    public AnexoDownload carregar(Long chamadoId, Long anexoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        AnexoChamado anexo = anexoRepository
                .findByIdAndChamadoIdAndChamadoEmpresaId(anexoId, chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado."));

        Path arquivo = diretorioRaiz.resolve(anexo.getCaminhoRelativo()).normalize();
        if (!arquivo.startsWith(diretorioRaiz)) {
            throw new IllegalStateException("Caminho de anexo inválido.");
        }

        try {
            Resource resource = new UrlResource(arquivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("Arquivo do anexo não encontrado.");
            }
            return new AnexoDownload(resource, anexo.getNomeOriginal(), anexo.getTipoMime());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Não foi possível carregar o anexo.", ex);
        }
    }

    private void salvar(Chamado chamado, MultipartFile arquivo) {
        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename()
        );

        if (nomeOriginal.contains("..") || nomeOriginal.contains("/") || nomeOriginal.contains("\\")) {
            throw new IllegalArgumentException("Nome de arquivo inválido.");
        }

        String extensao = extrairExtensao(nomeOriginal);
        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Extensões aceitas: " + EXTENSOES_PERMITIDAS
            );
        }

        String nomeArmazenado = UUID.randomUUID() + "." + extensao;
        Path subdiretorio = Path.of(
                chamado.getEmpresa().getId().toString(),
                chamado.getId().toString()
        );
        Path destino = diretorioRaiz.resolve(subdiretorio).resolve(nomeArmazenado).normalize();

        if (!destino.startsWith(diretorioRaiz)) {
            throw new IllegalArgumentException("Destino de arquivo inválido.");
        }

        try {
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            registrarLimpezaEmCasoDeRollback(destino);

            AnexoChamado anexo = new AnexoChamado();
            anexo.setChamado(chamado);
            anexo.setNomeOriginal(nomeOriginal);
            anexo.setNomeArmazenado(nomeArmazenado);
            anexo.setTipoMime(arquivo.getContentType());
            anexo.setTamanhoBytes(arquivo.getSize());
            anexo.setCaminhoRelativo(diretorioRaiz.relativize(destino).toString());

            anexoRepository.save(anexo);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao armazenar o arquivo " + nomeOriginal + ".", ex);
        }
    }

    private void registrarLimpezaEmCasoDeRollback(Path arquivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(arquivo);
                    } catch (IOException ignored) {
                        // Evita mascarar a exceção que provocou o rollback.
                    }
                }
            }
        });
    }

    private String extrairExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeArquivo.length() - 1) {
            throw new IllegalArgumentException("O arquivo precisa possuir uma extensão.");
        }
        return nomeArquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private void garantirChamadoNoTenant(Long chamadoId, Long empresaId) {
        chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));
    }

    private AnexoRespostaDTO toResposta(AnexoChamado anexo) {
        return new AnexoRespostaDTO(
                anexo.getId(),
                anexo.getNomeOriginal(),
                anexo.getTipoMime(),
                anexo.getTamanhoBytes(),
                anexo.getDataUpload()
        );
    }

    public record AnexoDownload(
            Resource resource,
            String nomeOriginal,
            String tipoMime
    ) {
    }
}
