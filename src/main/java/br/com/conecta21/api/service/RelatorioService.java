package br.com.conecta21.api.service;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private TenantContext tenantContext;

    public void exportarChamadosCsv(PrintWriter writer) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        exigirPermissaoRelatorio(ator);
        List<Chamado> chamados = chamadosVisiveis(ator);

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

    @Transactional(readOnly = true)
    public byte[] exportarChamadosPdf() throws IOException {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        exigirPermissaoRelatorio(ator);
        List<Chamado> chamados = chamadosVisiveis(ator);
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDPageContentStream conteudo = new PDPageContentStream(documento, pagina);
            float y = pagina.getMediaBox().getHeight() - 48;
            conteudo.setNonStrokingColor(17, 24, 39);
            escrever(conteudo, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16, 42, y, "Relatorio de chamados - Conecta21");
            y -= 28;
            conteudo.setNonStrokingColor(37, 99, 235);
            escrever(conteudo, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9, 42, y, "CHAMADO                         STATUS       PRIORIDADE     ABERTURA");
            y -= 15;
            conteudo.setNonStrokingColor(31, 41, 55);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Chamado chamado : chamados) {
                if (y < 70) {
                    conteudo.close();
                    pagina = new PDPage(PDRectangle.A4);
                    documento.addPage(pagina);
                    conteudo = new PDPageContentStream(documento, pagina);
                    y = pagina.getMediaBox().getHeight() - 48;
                }
                String categoria = chamado.getCategoria() == null ? "Sem categoria" : chamado.getCategoria().getNome();
                String abertura = chamado.getDataAbertura() == null ? "-" : chamado.getDataAbertura().format(formatter);
                escrever(conteudo, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8, 42, y,
                        String.format("#%d  %-12s %-14s %s", chamado.getId(), chamado.getStatus(), chamado.getPrioridade(), abertura));
                y -= 12;
                escrever(conteudo, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8, 56, y,
                        limitar("" + chamado.getTitulo() + " | " + categoria, 104));
                y -= 18;
            }
            if (chamados.isEmpty()) escrever(conteudo, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10, 42, y, "Nenhum chamado encontrado.");
            conteudo.close();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    private List<Chamado> chamadosVisiveis(Usuario ator) {
        List<Chamado> chamados = chamadoRepository.findAllByEmpresaId(ator.getEmpresa().getId());
        return ator.temPermissao("CHAMADOS_INTERNOS") ? chamados : chamados.stream().filter(c -> !c.isInterno()).toList();
    }

    private void exigirPermissaoRelatorio(Usuario ator) {
        if (!ator.temPermissao("GERAR_RELATORIOS")) throw new org.springframework.security.access.AccessDeniedException("Sem permissÃ£o para gerar relatÃ³rios.");
    }

    private void escrever(PDPageContentStream stream, org.apache.pdfbox.pdmodel.font.PDFont fonte, int tamanho, float x, float y, String texto) throws IOException {
        String seguro = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "?");
        stream.beginText(); stream.setFont(fonte, tamanho); stream.newLineAtOffset(x, y); stream.showText(seguro); stream.endText();
    }

    private String limitar(String texto, int tamanho) { return texto.length() <= tamanho ? texto : texto.substring(0, tamanho - 3) + "..."; }
}
