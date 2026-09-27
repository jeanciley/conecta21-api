package br.com.conecta21.api;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.service.ChamadoStatusAlteradoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MotorSlaJob {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void fiscalizarPrazosSla() {
        List<StatusChamado> statusPendentes = List.of(StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO);
        List<Chamado> chamadosAtrasados = chamadoRepository
                .findByStatusInAndDataLimiteResolucaoBefore(statusPendentes, LocalDateTime.now());

        if (!chamadosAtrasados.isEmpty()) {
            chamadosAtrasados.forEach(chamado -> {
                StatusChamado anterior = chamado.getStatus();
                chamado.setStatus(StatusChamado.EM_ATRASO);
                eventPublisher.publishEvent(new ChamadoStatusAlteradoEvent(
                        chamado.getId(),
                        chamado.getTitulo(),
                        chamado.getSolicitante().getEmail(),
                        chamado.getSolicitante().getNome(),
                        anterior,
                        StatusChamado.EM_ATRASO));
            });
            chamadoRepository.saveAll(chamadosAtrasados);
            System.out.println("Motor SLA: " + chamadosAtrasados.size() + " chamados atualizados para EM_ATRASO.");
        }
    }
}
