package br.com.conecta21.api.controller;

import br.com.conecta21.api.service.RelatorioService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @GetMapping("/chamados/csv")
    public void baixarRelatorioChamadoCsv(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"relatorio_chamados.csv\"");

        relatorioService.exportarChamadosCsv(response.getWriter());
    }

    @GetMapping(value = "/chamados/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixarRelatorioChamadoPdf() throws IOException {
        byte[] arquivo = relatorioService.exportarChamadosPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio_chamados.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(arquivo);
    }
}
