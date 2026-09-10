package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantContext tenantContext;
    private final AnexoChamadoService anexoChamadoService;
    private final ApplicationEventPublisher eventPublisher;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            TenantContext tenantContext,
            AnexoChamadoService anexoChamadoService,
            ApplicationEventPublisher eventPublisher) {
        this.chamadoRepository = chamadoRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tenantContext = tenantContext;
        this.anexoChamadoService = anexoChamadoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ChamadoRespostaDTO criar(ChamadoCriacaoDTO dto) {
        return criarInterno(dto, List.of());
    }

    @Transactional
    public ChamadoRespostaDTO criar(ChamadoCriacaoDTO dto, List<MultipartFile> arquivos) {
        return criarInterno(dto, arquivos == null ? List.of() : arquivos);
    }

    @Transactional(readOnly = true)
    public List<ChamadoRespostaDTO> listar() {
        return chamadoRepository.findAllByEmpresaId(tenantContext.getEmpresaIdAutenticada())
                .stream()
                .map(this::toResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChamadoRespostaDTO detalhar(Long id) {
        return toResposta(buscarNoTenant(id));
    }

    @Transactional
    public ChamadoRespostaDTO alterarStatus(Long id, ChamadoStatusDTO dto) {
        StatusChamado novoStatus = converterStatus(dto.status());

        Chamado chamado = buscarNoTenant(id);
        StatusChamado statusAnterior = chamado.getStatus();

        if (statusAnterior == novoStatus) {
            return toResposta(chamado);
        }

        chamado.setStatus(novoStatus);
        if (novoStatus == StatusChamado.RESOLVIDO) {
            if (chamado.getDataFechamento() == null) {
                chamado.setDataFechamento(LocalDateTime.now());
            }
        } else {
            chamado.setDataFechamento(null);
        }

        eventPublisher.publishEvent(new ChamadoStatusAlteradoEvent(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getSolicitante().getEmail(),
                chamado.getSolicitante().getNome(),
                statusAnterior,
                novoStatus
        ));

        return toResposta(chamado);
    }

    private ChamadoRespostaDTO criarInterno(ChamadoCriacaoDTO dto, List<MultipartFile> arquivos) {
        Usuario solicitante = tenantContext.getUsuarioAutenticado();
        Long empresaId = solicitante.getEmpresa().getId();

        Chamado chamado = new Chamado();
        chamado.setEmpresa(empresaRepository.getReferenceById(empresaId));
        chamado.setSolicitante(usuarioRepository.getReferenceById(solicitante.getId()));
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setStatus(StatusChamado.ABERTO);

        Chamado salvo = chamadoRepository.save(chamado);
        anexoChamadoService.salvarAnexos(salvo, arquivos);

        return toResposta(salvo);
    }

    private StatusChamado converterStatus(String status) {
        String normalizado = status.trim().toUpperCase(Locale.ROOT);

        // Compatibilidade com nomes usados pela versão anterior do ChamadoService.
        if ("EM_ATENDIMENTO".equals(normalizado)) {
            return StatusChamado.EM_ANDAMENTO;
        }
        if ("FECHADO".equals(normalizado)) {
            return StatusChamado.RESOLVIDO;
        }

        try {
            return StatusChamado.valueOf(normalizado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Status inválido. Valores aceitos: ABERTO, EM_ANDAMENTO, RESOLVIDO."
            );
        }
    }

    private Chamado buscarNoTenant(Long id) {
        return chamadoRepository.findByIdAndEmpresaId(id, tenantContext.getEmpresaIdAutenticada())
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));
    }

    private ChamadoRespostaDTO toResposta(Chamado chamado) {
        return new ChamadoRespostaDTO(
                chamado.getId(),
                chamado.getEmpresa().getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus().name(),
                chamado.getSolicitante().getId(),
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                chamado.getDataAbertura(),
                chamado.getDataFechamento());
    }
}
