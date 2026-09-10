package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.DashboardResponseDTO;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public DashboardResponseDTO obterMetricasUltimos30Dias() {

        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long empresaId = usuarioLogado.getEmpresa().getId();

        LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);

        long abertos = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.ABERTO, trintaDiasAtras);

        long emAndamento = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.EM_ANDAMENTO, trintaDiasAtras);

        long resolvidos = chamadoRepository.countByEmpresaIdAndStatusAndDataAberturaAfter(
                empresaId, StatusChamado.RESOLVIDO, trintaDiasAtras);

        return new DashboardResponseDTO(abertos, emAndamento, resolvidos);
    }
}
