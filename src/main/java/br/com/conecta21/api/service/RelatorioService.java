package br.com.conecta21.api.service;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RelatorioService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private TenantContext tenantContext;

    public void exportarChamadosCsv(PrintWriter writer) {
        List<Chamado> chamados = chamadoRepository.findAllByEmpresaId(tenantContext.getEmpresaIdAutenticada());

        writer.write('\uFEFF');

        writer.println("ID;Título;Status;Prioridade;Data Abertura;Data Limite SLA");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Chamado chamado : chamados) {

            String dataAbertura = chamado.getDataAbertura() != null ? chamado.getDataAbertura().format(formatter) : "N/A";
            String dataLimite = chamado.getDataLimiteResolucao() != null ? chamado.getDataLimiteResolucao().format(formatter) : "N/A";

            writer.println(
                    chamado.getId() + ";" +
                            chamado.getTitulo() + ";" +
                            chamado.getStatus() + ";" +
                            chamado.getPrioridade() + ";" +
                            dataAbertura + ";" +
                            dataLimite
            );
        }
    }
}
