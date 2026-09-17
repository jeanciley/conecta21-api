package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AnexoRespostaDTO;
import br.com.conecta21.api.model.AnexoChamado;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.repository.AnexoChamadoRepository;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
public class AnexoChamadoService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "txt", "log", "pdf"
    );

    private final AnexoChamadoRepository anexoRepository;
    private final ChamadoRepository chamadoRepository;
    private final TenantContext tenantContext;
    private final ArquivoStorageService storageService;

    public AnexoChamadoService(
            AnexoChamadoRepository anexoRepository,
            ChamadoRepository chamadoRepository,
            TenantContext tenantContext,
            ArquivoStorageService storageService) {
        this.anexoRepository = anexoRepository;
        this.chamadoRepository = chamadoRepository;
        this.tenantContext = tenantContext;
        this.storageService = storageService;
    }

    public void salvarAnexos(Chamado chamado, List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            return;
        }
        arquivos.stream()
                .filter(arquivo -> arquivo != null && !arquivo.isEmpty())
                .forEach(arquivo -> salvar(chamado, arquivo));
    }

    @Transactional
    public List<AnexoRespostaDTO> adicionar(Long chamadoId, List<MultipartFile> arquivos) {
        Chamado chamado = buscarChamadoNoTenant(chamadoId);
        salvarAnexos(chamado, arquivos);
        return listar(chamadoId);
    }

    @Transactional(readOnly = true)
    public List<AnexoRespostaDTO> listar(Long chamadoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        garantirChamadoNoTenant(chamadoId, empresaId);
        return anexoRepository
                .findAllByChamadoIdAndChamadoEmpresaIdOrderByDataUploadAsc(chamadoId, empresaId)
                .stream()
                .map(this::toResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnexoDownload carregar(Long chamadoId, Long anexoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        AnexoChamado anexo = anexoRepository
                .findByIdAndChamadoIdAndChamadoEmpresaId(anexoId, chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado."));

        Resource resource;
        try {
            resource = storageService.carregar(anexo.getCaminhoRelativo());
        } catch (IllegalArgumentException ex) {
            throw new EntityNotFoundException("Arquivo do anexo não encontrado.");
        }
        return new AnexoDownload(resource, anexo.getNomeOriginal(), anexo.getTipoMime());
    }

    private void salvar(Chamado chamado, MultipartFile arquivo) {
        ArquivoStorageService.ArquivoSalvo salvo = storageService.salvar(
                arquivo,
                Path.of("chamados", chamado.getEmpresa().getId().toString(), chamado.getId().toString()),
                EXTENSOES_PERMITIDAS);

        registrarLimpezaEmCasoDeRollback(salvo.caminhoRelativo());

        AnexoChamado anexo = new AnexoChamado();
        anexo.setChamado(chamado);
        anexo.setNomeOriginal(salvo.nomeOriginal());
        anexo.setNomeArmazenado(salvo.nomeArmazenado());
        anexo.setTipoMime(salvo.tipoMime());
        anexo.setTamanhoBytes(salvo.tamanhoBytes());
        anexo.setCaminhoRelativo(salvo.caminhoRelativo());
        anexoRepository.save(anexo);
    }

    private void registrarLimpezaEmCasoDeRollback(String caminhoRelativo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    storageService.removerSilenciosamente(caminhoRelativo);
                }
            }
        });
    }

    private Chamado buscarChamadoNoTenant(Long chamadoId) {
        return chamadoRepository.findByIdAndEmpresaId(chamadoId, tenantContext.getEmpresaIdAutenticada())
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));
    }

    private void garantirChamadoNoTenant(Long chamadoId, Long empresaId) {
        if (chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId).isEmpty()) {
            throw new EntityNotFoundException("Chamado não encontrado.");
        }
    }

    private AnexoRespostaDTO toResposta(AnexoChamado anexo) {
        return new AnexoRespostaDTO(
                anexo.getId(),
                anexo.getNomeOriginal(),
                anexo.getTipoMime(),
                anexo.getTamanhoBytes(),
                anexo.getDataUpload());
    }

    public record AnexoDownload(Resource resource, String nomeOriginal, String tipoMime) {
    }
}
