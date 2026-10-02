package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.DashboardResponseDTO;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional(readOnly = true)
    public DashboardResponseDTO obterMetricasUltimos30Dias() {
        var ator = tenantContext.getUsuarioAutenticado();
        if (ator.getPerfil() == PerfilUsuario.USUARIO && ator.temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Este perfil deve usar o painel de chamados internos.");
        }
        return obterMetricas(false);
    }

    @Transactional(readOnly = true)
    public DashboardResponseDTO obterMetricasChamadosInternos() {
        if (!tenantContext.getUsuarioAutenticado().temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Sem permissÃ£o para consultar chamados internos.");
        }
        return obterMetricas(true);
    }

    private DashboardResponseDTO obterMetricas(boolean interno) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        var ator = tenantContext.getUsuarioAutenticado();
        boolean equipe = ator.getPerfil() == PerfilUsuario.ADMIN || ator.getPerfil() == PerfilUsuario.TECNICO
                || ator.temPermissao("GERENCIAR_CHAMADOS");
        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);

        List<Chamado> chamadosRecentes = chamadoRepository.findAllByEmpresaId(empresaId).stream()
                .filter(chamado -> chamado.isInterno() == interno)
                .filter(chamado -> equipe || chamado.getSolicitante().getId().equals(ator.getId()))
                .filter(chamado -> chamado.getDataAbertura() != null && chamado.getDataAbertura().isAfter(trintaDiasAtras))
                .toList();

        long abertos = chamadosRecentes.stream().filter(c -> c.getStatus() == StatusChamado.ABERTO).count();
        long emAndamento = chamadosRecentes.stream().filter(c -> c.getStatus() == StatusChamado.EM_ANDAMENTO).count();
        long resolvidos = chamadosRecentes.stream().filter(c -> c.getStatus() == StatusChamado.RESOLVIDO).count();
        long emAtraso = chamadosRecentes.stream().filter(c -> c.getStatus() == StatusChamado.EM_ATRASO).count();

        Map<String, Long> chamadosPorCategoria = chamadosRecentes.stream()
                .filter(chamado -> chamado.getCategoria() != null)
                .collect(Collectors.groupingBy(chamado -> chamado.getCategoria().getNome(), Collectors.counting()));

        Map<String, Long> chamadosPorStatus = chamadosRecentes.stream().collect(Collectors.groupingBy(
                chamado -> chamado.getStatus().name(), Collectors.counting()));
        long slaCumpridos = chamadosRecentes.stream().filter(c -> c.getDataFechamento() != null
                && c.getDataLimiteResolucao() != null && !c.getDataFechamento().isAfter(c.getDataLimiteResolucao())).count();
        long slaViolados = chamadosRecentes.stream().filter(c -> c.getDataLimiteResolucao() != null
                && (c.getDataFechamento() != null ? c.getDataFechamento().isAfter(c.getDataLimiteResolucao())
                : c.getDataLimiteResolucao().isBefore(LocalDateTime.now()))).count();
        long slaRespostaCumprido = chamadosRecentes.stream().filter(c -> c.getDataLimiteResposta() != null
                && c.getDataPrimeiraResposta() != null && !c.getDataPrimeiraResposta().isAfter(c.getDataLimiteResposta())).count();
        long slaRespostaViolado = chamadosRecentes.stream().filter(c -> c.getDataLimiteResposta() != null
                && (c.getDataPrimeiraResposta() != null ? c.getDataPrimeiraResposta().isAfter(c.getDataLimiteResposta())
                : c.getDataLimiteResposta().isBefore(LocalDateTime.now()))).count();

        return new DashboardResponseDTO(abertos, emAndamento, resolvidos, emAtraso, chamadosPorCategoria,
                chamadosPorStatus, slaCumpridos, slaViolados, slaRespostaCumprido, slaRespostaViolado);
    }
}
