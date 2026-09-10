package br.com.conecta21.api;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MotorSlaJob {

    @Autowired
    private ChamadoRepository chamadoRepository;

    // A expressão Cron: "0 * * * * *" significa que roda no segundo ZERO de TODO minuto.
    // Ou seja, a cada 1 minuto o robô acorda.
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void fiscalizarPrazosSla() {
        // 1. Define os status que o robô deve olhar
        List<StatusChamado> statusPendentes = List.of(StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO);

        // 2. Busca no banco quem estourou o prazo comparado a agora
        List<Chamado> chamadosAtrasados = chamadoRepository
                .findByStatusInAndDataLimiteResolucaoBefore(statusPendentes, LocalDateTime.now());

        // 3. Atualiza todo mundo para EM_ATRASO
        if (!chamadosAtrasados.isEmpty()) {
            chamadosAtrasados.forEach(chamado -> chamado.setStatus(StatusChamado.EM_ATRASO));
            chamadoRepository.saveAll(chamadosAtrasados);

            // Um log no console só para você acompanhar o robô trabalhando
            System.out.println("🤖 Motor SLA: " + chamadosAtrasados.size() + " chamados atualizados para EM_ATRASO.");
        }
    }
}
