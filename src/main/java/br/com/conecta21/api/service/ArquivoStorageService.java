package br.com.conecta21.api.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoStorageService {

    private final Path diretorioRaiz;

    public ArquivoStorageService(@Value("${app.arquivos.diretorio:uploads}") String diretorioRaiz) {
        this.diretorioRaiz = Path.of(diretorioRaiz).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepararDiretorio() {
        try {
            Files.createDirectories(diretorioRaiz);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível preparar o diretório de arquivos.", ex);
        }
    }

    public ArquivoSalvo salvar(MultipartFile arquivo, Path subdiretorio, Set<String> extensoesPermitidas) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo enviado está vazio.");
        }

        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename());
        validarNome(nomeOriginal);

        String extensao = extrairExtensao(nomeOriginal);
        if (!extensoesPermitidas.contains(extensao)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido. Extensões aceitas: " + extensoesPermitidas);
        }

        String nomeArmazenado = UUID.randomUUID() + "." + extensao;
        Path destino = diretorioRaiz.resolve(subdiretorio).resolve(nomeArmazenado).normalize();
        garantirDentroDaRaiz(destino);

        try {
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return new ArquivoSalvo(
                    nomeOriginal,
                    nomeArmazenado,
                    arquivo.getContentType(),
                    arquivo.getSize(),
                    normalizarSeparadores(diretorioRaiz.relativize(destino).toString()));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao armazenar o arquivo " + nomeOriginal + ".", ex);
        }
    }

    public Resource carregar(String caminhoRelativo) {
        Path arquivo = resolver(caminhoRelativo);
        try {
            Resource resource = new UrlResource(arquivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Arquivo não encontrado no armazenamento.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Não foi possível carregar o arquivo.", ex);
        }
    }

    public void removerSilenciosamente(String caminhoRelativo) {
        if (caminhoRelativo == null || caminhoRelativo.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(caminhoRelativo));
        } catch (IOException | RuntimeException ignored) {
            // A limpeza física não deve mascarar o resultado da transação principal.
        }
    }

    private Path resolver(String caminhoRelativo) {
        Path arquivo = diretorioRaiz.resolve(caminhoRelativo).normalize();
        garantirDentroDaRaiz(arquivo);
        return arquivo;
    }

    private void validarNome(String nomeOriginal) {
        if (nomeOriginal.isBlank() || nomeOriginal.contains("..") || nomeOriginal.contains("/") || nomeOriginal.contains("\\")) {
            throw new IllegalArgumentException("Nome de arquivo inválido.");
        }
    }

    private String extrairExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeArquivo.length() - 1) {
            throw new IllegalArgumentException("O arquivo precisa possuir uma extensão.");
        }
        return nomeArquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private void garantirDentroDaRaiz(Path caminho) {
        if (!caminho.startsWith(diretorioRaiz)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
    }

    private String normalizarSeparadores(String caminho) {
        return caminho.replace('\\', '/');
    }

    public record ArquivoSalvo(
            String nomeOriginal,
            String nomeArmazenado,
            String tipoMime,
            long tamanhoBytes,
            String caminhoRelativo
    ) {
    }
}
