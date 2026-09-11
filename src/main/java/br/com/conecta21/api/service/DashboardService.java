package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.DashboardResponseDTO;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    public DashboardResponseDTO obterMetricasUltimos30Dias() {

        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);

        long abertos = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.ABERTO, trintaDiasAtras);

        long emAndamento = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.EM_ANDAMENTO, trintaDiasAtras);

        long resolvidos = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.RESOLVIDO, trintaDiasAtras);

        long emAtraso = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.EM_ATRASO, trintaDiasAtras);

        List<Chamado> chamadosRecentes = chamadoRepository.findAllByEmpresaId(empresaId);

        Map<String, Long> chamadosPorCategoria = chamadosRecentes.stream()
                .flatMap(chamado -> chamado.getCategorias().stream())
                .collect(Collectors.groupingBy(
                        Categoria::getNome,
                        Collectors.counting()
                ));

        return new DashboardResponseDTO(abertos, emAndamento, resolvidos, emAtraso, chamadosPorCategoria);
    }
}
