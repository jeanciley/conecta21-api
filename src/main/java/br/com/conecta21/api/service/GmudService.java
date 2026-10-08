package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.GmudCriacaoDTO;
import br.com.conecta21.api.dto.GmudFormularioDTO;
import br.com.conecta21.api.dto.GmudRespostaDTO;
import br.com.conecta21.api.dto.GmudResponsavelDTO;
import br.com.conecta21.api.model.*;
import br.com.conecta21.api.repository.AnexoChamadoRepository;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.GmudRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GmudService {

    private static final long MAX_EVIDENCIA_BYTES = 15L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 60L * 1024 * 1024;
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Set<String> EXTENSOES_XLSX = Set.of("xlsx");
    private static final Set<String> EXTENSOES_EVIDENCIA = Set.of("png", "jpg", "jpeg");
    private static final List<String> AMBIENTES = List.of("PRODUCAO", "HOMOLOGACAO", "DESENVOLVIMENTO");
    private static final List<PerfilUsuario> PERFIS_RESPONSAVEIS = List.of(PerfilUsuario.ADMIN, PerfilUsuario.TECNICO);

    private final GmudRepository gmudRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloService moduloService;
    private final AnexoChamadoRepository anexoRepository;
    private final TenantContext tenantContext;
    private final ArquivoStorageService storageService;
    private final TemplateService templateService;

    public GmudService(GmudRepository gmudRepository, ChamadoRepository chamadoRepository,
                       UsuarioRepository usuarioRepository, ModuloService moduloService,
                       AnexoChamadoRepository anexoRepository, TenantContext tenantContext,
                       ArquivoStorageService storageService, TemplateService templateService) {
        this.gmudRepository = gmudRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloService = moduloService;
        this.anexoRepository = anexoRepository;
        this.tenantContext = tenantContext;
        this.storageService = storageService;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public GmudFormularioDTO obterFormulario(Long ticketId) {
        Long empresaId = exigirModuloETecnico();
        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(ticketId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado n\u00e3o encontrado."));
        Usuario usuarioAtual = tenantContext.getUsuarioAutenticado();
        List<Usuario> responsaveis = usuarioRepository
                .findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(empresaId, PERFIS_RESPONSAVEIS);
        Long responsavelAtribuido = chamado.getTecnico() != null ? chamado.getTecnico().getId() : usuarioAtual.getId();
        boolean atribuicaoValida = responsaveis.stream().anyMatch(usuario -> usuario.getId().equals(responsavelAtribuido));
        Long sugerido = atribuicaoValida ? responsavelAtribuido : usuarioAtual.getId();
        return new GmudFormularioDTO(
                chamado.getId(), "#" + chamado.getId(), chamado.getEmpresa().getNomeFantasia(),
                chamado.getSolicitante().getNome(),
                chamado.getTecnico() == null ? "N\u00e3o atribu\u00eddo" : chamado.getTecnico().getNome(),
                sugerido,
                templateService.getTemplates().stream().map(TemplateService.TemplateInfo::label).toList(),
                responsaveis.stream().map(usuario -> new GmudResponsavelDTO(
                        usuario.getId(), usuario.getNome(), usuario.getPerfil().name())).toList(),
                templateService.getTemplates());
    }

    @Transactional
    public GmudRespostaDTO criar(Long ticketId, GmudCriacaoDTO dados, List<MultipartFile> evidencias) {
        Long empresaId = exigirModuloETecnico();
        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(ticketId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado n\u00e3o encontrado."));
        validarEvidencias(evidencias);

        String modelo = templateService.getTemplates().stream().map(TemplateService.TemplateInfo::label)
                .filter(opcao -> opcao.equalsIgnoreCase(dados.modeloUtilizado().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Modelo de GMUD inv\u00e1lido."));
        String ambiente = dados.ambiente().trim().toUpperCase(Locale.ROOT);
        if (!AMBIENTES.contains(ambiente)) {
            throw new IllegalArgumentException("Ambiente inv\u00e1lido.");
        }

        Usuario responsavel = usuarioRepository
                .findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(empresaId, PERFIS_RESPONSAVEIS)
                .stream().filter(usuario -> usuario.getId().equals(dados.responsavelId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Respons\u00e1vel inv\u00e1lido para este cliente."));

        Gmud gmud = new Gmud();
        gmud.setEmpresa(chamado.getEmpresa());
        gmud.setChamado(chamado);
        gmud.setResponsavel(responsavel);
        gmud.setModeloUtilizado(modelo);
        gmud.setAmbiente(ambiente);
        gmud.setDataAgendada(dados.dataAgendada());
        gmud.setRiscosImpactos(dados.riscosImpactos().trim());
        gmud.setStatusAprovacao("PENDENTE");
        gmud = gmudRepository.saveAndFlush(gmud);

        List<String> nomesEvidencias = salvarEvidencias(gmud, chamado, evidencias);
        byte[] planilha = templateService.generate(criarRequisicaoTemplate(gmud), evidencias, List.of());
        String nomeArquivo = "GMUD_" + chamado.getId() + ".xlsx";
        ArquivoStorageService.ArquivoSalvo arquivo = storageService.salvar(
                planilha, nomeArquivo, MIME_XLSX,
                Path.of("chamados", empresaId.toString(), chamado.getId().toString(), "gmud"),
                EXTENSOES_XLSX);
        registrarLimpezaEmCasoDeRollback(arquivo.caminhoRelativo());

        AnexoChamado anexo = criarAnexo(chamado, gmud, arquivo);
        gmud.setCaminhoArquivoGerado(arquivo.caminhoRelativo());
        gmudRepository.save(gmud);
        return resposta(gmud, anexo.getId(), nomeArquivo);
    }

    private br.com.conecta21.api.dto.GmudTemplateRequest criarRequisicaoTemplate(Gmud gmud) {
        var request = new br.com.conecta21.api.dto.GmudTemplateRequest();
        var template = templateService.getTemplates().stream()
                .filter(item -> item.label().equalsIgnoreCase(gmud.getModeloUtilizado()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Modelo de GMUD inv\u00e1lido."));
        request.templateId = template.id();
        request.operatorName = gmud.getResponsavel().getNome();
        request.operatorId = gmud.getResponsavel().getId();
        request.requestDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        request.clientName = gmud.getEmpresa().getNomeFantasia();
        request.requester = gmud.getChamado().getSolicitante().getNome();
        request.ticket = "#" + gmud.getChamado().getId();
        request.classification = "Mudan\u00e7a";
        request.description = template.label();
        request.clientContact.name = request.requester;
        request.clientContact.email = gmud.getChamado().getSolicitante().getEmail();
        request.operatorContact.name = gmud.getResponsavel().getNome();
        request.operatorContact.role = gmud.getResponsavel().getPerfil().name();
        request.operatorContact.email = gmud.getResponsavel().getEmail();
        request.environment = switch (gmud.getAmbiente()) {
            case "PRODUCAO" -> "PRODU\u00C7\u00C3O";
            case "HOMOLOGACAO" -> "HOMOLOGA\u00C7\u00C3O";
            default -> "DESENVOLVIMENTO";
        };
        request.executionDate = gmud.getDataAgendada().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        request.executionTime = gmud.getDataAgendada().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        request.risksImpacts = gmud.getRiscosImpactos();
        return request;
    }

    @Transactional
    public GmudRespostaDTO criarComTemplate(Long ticketId, br.com.conecta21.api.dto.GmudTemplateRequest request,
            List<MultipartFile> attachments, List<MultipartFile> prints, MultipartFile appManagerPrint,
            MultipartFile integrationPrint, MultipartFile tomcatPrint) {
        Long empresaId = exigirModuloETecnico();
        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(ticketId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado n\u00e3o encontrado."));
        List<MultipartFile> todos = new java.util.ArrayList<>();
        if (attachments != null) todos.addAll(attachments);
        if (prints != null) todos.addAll(prints);
        for (MultipartFile item : new MultipartFile[]{appManagerPrint, integrationPrint, tomcatPrint}) {
            if (item != null && !item.isEmpty()) todos.add(item);
        }
        validarEvidencias(todos);
        List<Usuario> responsaveis = usuarioRepository.findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(empresaId, PERFIS_RESPONSAVEIS);
        Usuario responsavel = responsaveis.stream().filter(u -> u.getId().equals(request.operatorId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Respons\u00e1vel inv\u00e1lido para este cliente."));
        var template = templateService.getTemplates().stream().filter(t -> t.id().equals(request.templateId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Modelo de GMUD inv\u00e1lido."));
        if (request.executionDate == null || request.executionDate.isBlank() || request.executionTime == null || request.executionTime.isBlank())
            throw new IllegalArgumentException("Informe a data e o hor\u00e1rio da execu\u00e7\u00e3o.");
        java.time.LocalDateTime agenda;
        try { agenda = java.time.LocalDateTime.of(java.time.LocalDate.parse(request.executionDate), java.time.LocalTime.parse(request.executionTime)); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Data ou hor\u00e1rio de execu\u00e7\u00e3o inv\u00e1lido."); }
        if (agenda.isBefore(java.time.LocalDateTime.now())) throw new IllegalArgumentException("A agenda de execu\u00e7\u00e3o deve ser futura.");
        if (request.risksImpacts == null || request.risksImpacts.isBlank()) throw new IllegalArgumentException("Informe os riscos e impactos da mudan\u00e7a.");
        String ambiente = normalizeEnvironmentCode(request.environment);

        Gmud gmud = new Gmud();
        gmud.setEmpresa(chamado.getEmpresa()); gmud.setChamado(chamado); gmud.setResponsavel(responsavel);
        gmud.setModeloUtilizado(template.label()); gmud.setAmbiente(ambiente); gmud.setDataAgendada(agenda);
        gmud.setRiscosImpactos(request.risksImpacts.trim()); gmud.setStatusAprovacao("PENDENTE");
        gmud = gmudRepository.saveAndFlush(gmud);

        request.operatorId = responsavel.getId(); request.operatorName = responsavel.getNome();
        request.clientName = chamado.getEmpresa().getNomeFantasia();
        request.requester = chamado.getSolicitante().getNome(); request.ticket = "#" + chamado.getId();
        request.description = template.label();
        request.operatorContact.name = responsavel.getNome(); request.operatorContact.email = responsavel.getEmail();
        request.operatorContact.role = responsavel.getPerfil().name();
        request.clientContact.name = chamado.getSolicitante().getNome(); request.clientContact.email = chamado.getSolicitante().getEmail();
        List<String> nomes = salvarEvidencias(gmud, chamado, todos);
        byte[] planilha = templateService.generate(request, attachments, prints, appManagerPrint, integrationPrint, tomcatPrint);
        planilha = anexarResumoConecta21(planilha, gmud, nomes);
        String nomeArquivo = "GMUD_" + chamado.getId() + ".xlsx";
        var arquivo = storageService.salvar(planilha, nomeArquivo, MIME_XLSX,
                Path.of("chamados", empresaId.toString(), chamado.getId().toString(), "gmud"), EXTENSOES_XLSX);
        registrarLimpezaEmCasoDeRollback(arquivo.caminhoRelativo());
        AnexoChamado anexo = criarAnexo(chamado, gmud, arquivo);
        gmud.setCaminhoArquivoGerado(arquivo.caminhoRelativo()); gmudRepository.save(gmud);
        return resposta(gmud, anexo.getId(), nomeArquivo);
    }

    private String normalizeEnvironmentCode(String value) {
        String normalized = java.text.Normalizer.normalize(value == null ? "" : value.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        if (normalized.equals("PRODUCAO") || normalized.equals("HOMOLOGACAO") || normalized.equals("DESENVOLVIMENTO")) return normalized;
        throw new IllegalArgumentException("Ambiente inv\u00e1lido.");
    }

    private byte[] anexarResumoConecta21(byte[] original, Gmud gmud, List<String> evidencias) {
        try (var input = new java.io.ByteArrayInputStream(original);
             var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(input);
             var output = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Registro Conecta21");
            String[][] linhas = {
                {"Campo", "Valor"}, {"Chamado", "#" + gmud.getChamado().getId()},
                {"Cliente", gmud.getEmpresa().getNomeFantasia()}, {"Solicitante", gmud.getChamado().getSolicitante().getNome()},
                {"Respons\u00e1vel", gmud.getResponsavel().getNome()}, {"Modelo", gmud.getModeloUtilizado()},
                {"Ambiente", gmud.getAmbiente()}, {"Agenda", gmud.getDataAgendada().toString()},
                {"Riscos e impactos", gmud.getRiscosImpactos()},
                {"Evid\u00eancias", evidencias.isEmpty() ? "Nenhuma" : String.join("; ", evidencias)}
            };
            for (int i = 0; i < linhas.length; i++) { var row = sheet.createRow(i); row.createCell(0).setCellValue(linhas[i][0]); row.createCell(1).setCellValue(linhas[i][1]); }
            sheet.setColumnWidth(0, 26 * 256); sheet.setColumnWidth(1, 90 * 256);
            workbook.write(output); return output.toByteArray();
        } catch (Exception ex) { throw new IllegalStateException("N\u00e3o foi poss\u00edvel registrar os dados da GMUD na planilha.", ex); }
    }

    @Transactional(readOnly = true)
    public GmudDownload baixarArquivo(Long gmudId) {
        Long empresaId = exigirModuloETecnico();
        Gmud gmud = gmudRepository.findByIdAndEmpresaId(gmudId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("GMUD n\u00e3o encontrada."));
        Resource resource = storageService.carregar(gmud.getCaminhoArquivoGerado());
        return new GmudDownload(resource, "GMUD_" + gmud.getChamado().getId() + ".xlsx", MIME_XLSX);
    }

    private Long exigirModuloETecnico() {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuario.ADMIN && usuario.getPerfil() != PerfilUsuario.TECNICO) {
            throw new org.springframework.security.access.AccessDeniedException("GMUD dispon\u00edvel somente para t\u00e9cnicos e administradores.");
        }
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        boolean ativo = moduloService.estaAtivo(empresaId, "GMUD");
        if (!ativo) {
            throw new org.springframework.security.access.AccessDeniedException("O m\u00f3dulo GMUD n\u00e3o est\u00e1 contratado para este cliente.");
        }
        return empresaId;
    }

    private void validarEvidencias(List<MultipartFile> evidencias) {
        if (evidencias == null || evidencias.isEmpty()) return;
        long total = 0;
        for (MultipartFile arquivo : evidencias) {
            if (arquivo == null || arquivo.isEmpty()) continue;
            if (arquivo.getSize() > MAX_EVIDENCIA_BYTES) {
                throw new IllegalArgumentException("Cada evid\u00eancia deve ter no m\u00e1ximo 15 MB.");
            }
            total += arquivo.getSize();
            if (total > MAX_TOTAL_BYTES) {
                throw new IllegalArgumentException("O total de evid\u00eancias por requisi\u00e7\u00e3o n\u00e3o pode exceder 60 MB.");
            }
            String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
            int ponto = nome.lastIndexOf('.');
            String extensao = ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
            String mime = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase(Locale.ROOT);
            if (!EXTENSOES_EVIDENCIA.contains(extensao)
                    || !("image/png".equals(mime) || "image/jpeg".equals(mime))) {
                throw new IllegalArgumentException("As evid\u00eancias devem estar no formato PNG ou JPG.");
            }
            validarAssinaturaImagem(arquivo, extensao);
        }
    }

    private void validarAssinaturaImagem(MultipartFile arquivo, String extensao) {
        try (var input = arquivo.getInputStream()) {
            byte[] header = input.readNBytes(8);
            boolean png = "png".equals(extensao) && header.length == 8
                    && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
            boolean jpeg = ("jpg".equals(extensao) || "jpeg".equals(extensao))
                    && header.length >= 3 && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            if (!png && !jpeg) throw new IllegalArgumentException("O conte\u00fado do arquivo n\u00e3o corresponde a uma imagem PNG/JPG v\u00e1lida.");
        } catch (IOException ex) {
            throw new IllegalArgumentException("N\u00e3o foi poss\u00edvel validar uma evid\u00eancia.", ex);
        }
    }

    private List<String> salvarEvidencias(Gmud gmud, Chamado chamado, List<MultipartFile> evidencias) {
        if (evidencias == null) return List.of();
        return evidencias.stream().filter(arquivo -> arquivo != null && !arquivo.isEmpty()).map(arquivo -> {
            ArquivoStorageService.ArquivoSalvo salvo = storageService.salvar(arquivo,
                    Path.of("chamados", gmud.getEmpresa().getId().toString(), chamado.getId().toString()),
                    EXTENSOES_EVIDENCIA);
            registrarLimpezaEmCasoDeRollback(salvo.caminhoRelativo());
            criarAnexo(chamado, gmud, salvo);
            return salvo.nomeOriginal();
        }).toList();
    }

    private AnexoChamado criarAnexo(Chamado chamado, Gmud gmud, ArquivoStorageService.ArquivoSalvo salvo) {
        AnexoChamado anexo = new AnexoChamado();
        anexo.setChamado(chamado);
        anexo.setGmud(gmud);
        anexo.setNomeOriginal(salvo.nomeOriginal());
        anexo.setNomeArmazenado(salvo.nomeArmazenado());
        anexo.setTipoMime(salvo.tipoMime());
        anexo.setTamanhoBytes(salvo.tamanhoBytes());
        anexo.setCaminhoRelativo(salvo.caminhoRelativo());
        return anexoRepository.save(anexo);
    }

    private void registrarLimpezaEmCasoDeRollback(String caminhoRelativo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    storageService.removerSilenciosamente(caminhoRelativo);
                }
            }
        });
    }

    private GmudRespostaDTO resposta(Gmud gmud, Long anexoId, String nomeArquivo) {
        return new GmudRespostaDTO(gmud.getId(), gmud.getChamado().getId(), gmud.getResponsavel().getId(),
                gmud.getResponsavel().getNome(), gmud.getModeloUtilizado(), gmud.getAmbiente(),
                gmud.getDataAgendada(), gmud.getRiscosImpactos(), gmud.getStatusAprovacao(), nomeArquivo, anexoId);
    }

    public record GmudDownload(Resource resource, String nomeArquivo, String mimeType) {}
}
