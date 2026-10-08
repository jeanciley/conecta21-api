package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.GmudTemplateRequest;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TemplateService {
    private static final String TEMPLATE_PATH = "gmud/modelos.xlsx";
    private static final int CREATED_EVIDENCE_CONTENT_ROWS = 28;
    private static final int GENERIC_EVIDENCE_START_COL = 1; // B
    private static final int GENERIC_EVIDENCE_END_COL_EXCLUSIVE = 17; // Q + 1
    private static final double IMAGE_PADDING_PX = 10d;

    private static final List<TemplateSpec> TEMPLATE_SPECS = List.of(
            new TemplateSpec("gc-tomcat-wsi", "GC Tomcat - WSI", "GC Tomcat - WSI"),
            new TemplateSpec("atualizacao-tasy", "Atualização Tasy", "Atualização TASY"),
            new TemplateSpec("atualizacao-tasy-emergencial", "Atualização Tasy - Emergencial", "Atualização TASY - Emergencial"),
            new TemplateSpec("atualizacao-certificado-tie", "Atualização de Certificado Tie", "Atualização Certificado TIE"),
            new TemplateSpec("atualizacao-tomcat-9", "Atualização Tomcat 9", "Atualização Tomcat 9"),
            new TemplateSpec("atualizacao-tomcat-tasy-interfaces", "Atualização Tomcat Tasy Interfaces", "Atualização Tomcat Tasy Interfa"),
            new TemplateSpec("versao-app-manager-mesmo-tema", "Versão App Manager - Mesmo Tema", "Versão App Manager - Mesmo Tema"),
            new TemplateSpec("recurso-oci", "Recurso OCI", "Recurso OCI"),
            new TemplateSpec("versao-app-manager-novo-tema", "Versão App Manager - Novo Tema", "Versão App Manager - Novo tema"),
            new TemplateSpec("troca-certificado-ssl", "Troca de certificado SSL", "Troca Certificado SSL"),
            new TemplateSpec("reinicializacao-aplicacoes", "Reinicialização de Aplicações", "Reinicialização de aplicações")
    );

    private static final Set<String> ENVIRONMENTS = Set.of("PRODUÇÃO", "HOMOLOGAÇÃO", "DESENVOLVIMENTO");

    private final UsuarioRepository usuarioRepository;
    private final TenantContext tenantContext;

    public TemplateService(UsuarioRepository usuarioRepository, TenantContext tenantContext) {
        this.usuarioRepository = usuarioRepository;
        this.tenantContext = tenantContext;
    }

    public List<OperatorInfo> getOperators() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        return usuarioRepository.findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(
                empresaId, List.of(PerfilUsuario.ADMIN, PerfilUsuario.TECNICO)).stream()
                .map(usuario -> new OperatorInfo(usuario.getId(), usuario.getNome(), "", usuario.getPerfil().name(), usuario.getEmail()))
                .toList();
    }

    public List<TemplateInfo> getTemplates() {
        List<OperatorInfo> operators = getOperators();
        Set<String> operatorNames = normalizedOperatorNames(operators);
        try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<TemplateInfo> result = new ArrayList<>();

            for (TemplateSpec spec : TEMPLATE_SPECS) {
                Sheet sheet = findSheet(workbook, spec.sheetName());
                if (sheet == null) continue;
                result.add(new TemplateInfo(
                        spec.id(), spec.label(), spec.sheetName(),
                        extractRisks(sheet, formatter, evaluator),
                        extractActivities(sheet, formatter, evaluator, List.of("Atividades do Plano de testes"), operatorNames),
                        extractActivities(sheet, formatter, evaluator, List.of("Atividades do Plano de execução"), operatorNames),
                        extractRollback(sheet, formatter, evaluator, operatorNames),
                        extractAgenda(sheet, formatter, evaluator),
                        extractEnvironmentDetails(sheet, formatter, evaluator),
                        extractTasyDetails(spec, sheet)
                ));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar os templates da GMUD", e);
        }
    }

    public byte[] generate(GmudTemplateRequest request) {
        return generate(request, List.of(), List.of(), null, null, null);
    }

    public byte[] generate(GmudTemplateRequest request, List<MultipartFile> attachments, List<MultipartFile> prints) {
        return generate(request, attachments, prints, null, null, null);
    }

    public byte[] generate(GmudTemplateRequest request, List<MultipartFile> attachments, List<MultipartFile> prints,
                           MultipartFile tasyAppManagerPrint, MultipartFile tasyIntegrationPrint, MultipartFile tasyTomcatPrint) {
        TemplateSpec spec = resolveTemplate(request.templateId);
        if (!spec.label().equals(request.description)) {
            throw new IllegalArgumentException("A descrição deve corresponder ao template selecionado.");
        }

        List<OperatorInfo> operators = getOperators();
        OperatorInfo operator = resolveOperator(request.operatorName, operators);
        String environment = normalizeEnvironment(request.environment);

        try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = findSheet(workbook, spec.sheetName());
            if (sheet == null) throw new IllegalArgumentException("A aba do template não foi encontrada: " + spec.sheetName());

            // Dados da solicitação, acima da grade de serviços.
            put(sheet, "J5", request.clientName);
            putDate(sheet, "J6", request.requestDate);
            put(sheet, "J7", request.requester);
            put(sheet, "G10", request.ticket);
            put(sheet, "G11", request.classification);
            put(sheet, "G12", spec.label());

            // Preenche os contatos diretamente e remove as fórmulas externas quebradas (#REF!) do template.
            fillClientContact(sheet, request.clientContact);
            fillOperatorContact(sheet, operator);
            if (isStandardTasy(spec)) {
                fillTasySpecificData(sheet, request.tasyDetails);
            }

            // Alterna os responsáveis das atividades antes da expansão da grade de serviços.
            fillActivityResponsibles(sheet, operator, request.clientContact, normalizedOperatorNames(operators));

            // Agenda agora é editável, mantendo rótulos, estilos e posição do template.
            fillAgenda(sheet, request.agenda);

            int environmentRow = findRowInColumnExact(sheet, 1, "Classificação do Ambiente:");
            if (environmentRow < 0) throw new IllegalArgumentException("Campo de ambiente não encontrado no template: " + spec.label());
            put(sheet, "G" + (environmentRow + 1), environment);

            // Serviços podem crescer além das linhas originalmente reservadas no Excel.
            fillServices(sheet, request.services == null ? List.of() : request.services);

            // Imagens são incluídas após todos os deslocamentos de linha para manter as âncoras corretas.
            embedEvidenceImages(workbook, sheet, spec,
                    attachments == null ? List.of() : attachments,
                    prints == null ? List.of() : prints,
                    tasyAppManagerPrint, tasyIntegrationPrint, tasyTomcatPrint);

            int selectedIndex = workbook.getSheetIndex(sheet);
            workbook.setActiveSheet(selectedIndex);
            workbook.setSelectedTab(selectedIndex);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(out);
            return out.toByteArray();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar GMUD para o template " + spec.label(), e);
        }
    }

    private Sheet findSheet(Workbook workbook, String expectedName) {
        String expected = nullToEmpty(expectedName).trim();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            if (sheet.getSheetName().trim().equalsIgnoreCase(expected)) return sheet;
        }
        return null;
    }

    private TemplateSpec resolveTemplate(String templateId) {
        return TEMPLATE_SPECS.stream()
                .filter(x -> x.id().equalsIgnoreCase(nullToEmpty(templateId)) || x.label().equalsIgnoreCase(nullToEmpty(templateId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Template inválido. Selecione uma descrição disponível na lista."));
    }

    private OperatorInfo resolveOperator(String operatorName, List<OperatorInfo> operators) {
        return operators.stream()
                .filter(x -> x.name().equalsIgnoreCase(nullToEmpty(operatorName).trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Operador inválido. Selecione um operador cadastrado."));
    }

    private String normalizeEnvironment(String environment) {
        String value = nullToEmpty(environment).trim().toUpperCase(Locale.ROOT);
        if (!ENVIRONMENTS.contains(value)) {
            throw new IllegalArgumentException("Ambiente inválido. Use PRODUÇÃO, HOMOLOGAÇÃO ou DESENVOLVIMENTO.");
        }
        return value;
    }

    private void fillServices(Sheet sheet, List<GmudTemplateRequest.ServiceAsset> items) {
        final int firstDataRow = 16; // número visual do Excel
        int contactSectionRow = findRowInColumnExact(sheet, 1, "Informações de Contato");
        if (contactSectionRow < 0) throw new IllegalArgumentException("Seção de contatos não encontrada no template.");

        int firstDataRowIndex = firstDataRow - 1;
        int originalLastDataRow = contactSectionRow - 2;
        int originalCapacity = originalLastDataRow - firstDataRowIndex + 1;
        if (originalCapacity < 1) throw new IllegalArgumentException("Grade de serviços inválida no template.");

        int required = Math.max(items.size(), originalCapacity);
        int extraRows = Math.max(0, required - originalCapacity);
        if (extraRows > 0) {
            int insertAt = originalLastDataRow + 1;
            insertRowsLike(sheet, originalLastDataRow, insertAt, extraRows);
        }

        int finalCapacity = originalCapacity + extraRows;
        for (int i = 0; i < finalCapacity; i++) {
            int excelRow = firstDataRow + i;
            blankCell(sheet, "B" + excelRow);
            blankCell(sheet, "K" + excelRow);
        }

        for (int i = 0; i < items.size(); i++) {
            GmudTemplateRequest.ServiceAsset item = items.get(i);
            int excelRow = firstDataRow + i;
            put(sheet, "B" + excelRow, item.service);
            put(sheet, "K" + excelRow, item.asset);
        }
    }

    private void insertRowsLike(Sheet sheet, int sourceRowIndex, int insertAtIndex, int count) {
        Row source = sheet.getRow(sourceRowIndex);
        if (source == null) source = sheet.createRow(sourceRowIndex);

        List<CellRangeAddress> sourceMerges = new ArrayList<>();
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == sourceRowIndex && region.getLastRow() == sourceRowIndex) {
                sourceMerges.add(new CellRangeAddress(region.getFirstRow(), region.getLastRow(), region.getFirstColumn(), region.getLastColumn()));
            }
        }

        if (insertAtIndex <= sheet.getLastRowNum()) {
            sheet.shiftRows(insertAtIndex, sheet.getLastRowNum(), count, true, false);
        }

        for (int i = 0; i < count; i++) {
            int targetIndex = insertAtIndex + i;
            Row target = sheet.createRow(targetIndex);
            target.setHeight(source.getHeight());
            if (source.getRowStyle() != null) target.setRowStyle(source.getRowStyle());

            short lastCell = source.getLastCellNum();
            int cellLimit = lastCell < 0 ? 17 : Math.max(17, lastCell);
            for (int c = 0; c < cellLimit; c++) {
                Cell sourceCell = source.getCell(c);
                Cell targetCell = target.createCell(c, CellType.BLANK);
                if (sourceCell != null) targetCell.setCellStyle(sourceCell.getCellStyle());
            }

            for (CellRangeAddress merge : sourceMerges) {
                int delta = targetIndex - sourceRowIndex;
                sheet.addMergedRegion(new CellRangeAddress(
                        merge.getFirstRow() + delta,
                        merge.getLastRow() + delta,
                        merge.getFirstColumn(),
                        merge.getLastColumn()
                ));
            }
        }
    }

    private void fillClientContact(Sheet sheet, GmudTemplateRequest.Contact contact) {
        int headingRow = findRowInColumnExact(sheet, 1, "Responsável no cliente");
        if (headingRow < 0) throw new IllegalArgumentException("Contato do cliente não encontrado no template.");
        fillContactAt(sheet, headingRow + 2, contact == null ? new GmudTemplateRequest.Contact() : contact);
    }

    private void fillOperatorContact(Sheet sheet, OperatorInfo operator) {
        int headingRow = findFirstRowStartingWith(sheet, "Responsável Redix");
        if (headingRow < 0) throw new IllegalArgumentException("Contato do operador não encontrado no template.");
        int excelNameRow = headingRow + 2;
        put(sheet, "D" + excelNameRow, operator.name());
        put(sheet, "D" + (excelNameRow + 1), operator.phone());
        put(sheet, "N" + excelNameRow, operator.role());
        put(sheet, "N" + (excelNameRow + 1), operator.email());
    }

    private boolean isStandardTasy(TemplateSpec spec) {
        return spec != null && "atualizacao-tasy".equals(spec.id());
    }

    private void fillTasySpecificData(Sheet sheet, GmudTemplateRequest.TasyDetails details) {
        if (details == null) return;

        GmudTemplateRequest.Contact databaseContact = details.databaseContact;
        if (hasAnyContactData(databaseContact)) {
            int headingRow = findRowInColumnExact(sheet, 1, "Responsável Redix - Time de Banco de Dados");
            if (headingRow < 0) {
                throw new IllegalArgumentException("Responsável do Time de Banco de Dados não encontrado no template Atualização Tasy.");
            }
            fillContactAt(sheet, headingRow + 2, databaseContact);
        }

        Cell stopCell = findCellInColumnStartingWith(sheet, 1, "Baixar os serviços de integração:");
        if (stopCell != null && (!isBlank(details.tieStopDetails) || !isBlank(details.tomcatIntegrationStopDetails))) {
            writeTasyStopActivity(stopCell, details.tieStopDetails, details.tomcatIntegrationStopDetails);
        }

        Cell removeCell = findCellInColumnStartingWith(sheet, 1, "Remover imagens da antiga versão");
        if (removeCell != null && !isBlank(details.removedImages)) {
            writeRedTail(removeCell, "Remover imagens da antiga versão ", details.removedImages);
        }

        Cell addCell = findCellInColumnStartingWith(sheet, 1, "Adcionar imagens da nova versão");
        if (addCell != null && !isBlank(details.addedImages)) {
            writeRedTail(addCell, "Adcionar imagens da nova versão ", details.addedImages);
        }

        Cell artifactsCell = findCellInColumnStartingWith(sheet, 1, "Alterar artefatos nos servidores de integração");
        if (artifactsCell != null && !isBlank(details.integrationArtifacts)) {
            writeRedTail(artifactsCell,
                    "Alterar artefatos nos servidores de integração, contemplando os seguintes artefatos:\n",
                    details.integrationArtifacts);
        }
    }

    private boolean hasAnyContactData(GmudTemplateRequest.Contact contact) {
        return contact != null && (!isBlank(contact.name) || !isBlank(contact.phone) || !isBlank(contact.role) || !isBlank(contact.email));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void writeTasyStopActivity(Cell target, String tieDetails, String tomcatDetails) {
        XSSFRichTextString original = richText(target);
        String originalText = original.getString();
        String tiePrefix = "TIE - Parada do serviço do Tasy Interfaces - ";
        String tomcatPrefix = "\nTomcat - Parada dos serviços do Tomcat Integração - ";
        String defaultTie = between(originalText, tiePrefix, tomcatPrefix);
        String defaultTomcat = substringAfter(originalText, tomcatPrefix);

        String tieValue = isBlank(tieDetails) ? defaultTie : tieDetails.trim();
        String tomcatValue = isBlank(tomcatDetails) ? defaultTomcat : tomcatDetails.trim();
        XSSFFont blackBold = fontAtText(original, tiePrefix);
        XSSFFont redBold = fontAtText(original, defaultTie);

        XSSFRichTextString updated = new XSSFRichTextString("Baixar os serviços de integração:\n");
        appendRich(updated, tiePrefix, blackBold);
        appendRich(updated, tieValue, redBold);
        appendRich(updated, tomcatPrefix, blackBold);
        appendRich(updated, tomcatValue, redBold);
        target.setCellValue(updated);
    }

    private void writeRedTail(Cell target, String prefix, String value) {
        XSSFRichTextString original = richText(target);
        String originalText = original.getString();
        String previousTail = substringAfter(originalText, prefix);
        XSSFFont redFont = fontAtText(original, previousTail);
        XSSFRichTextString updated = new XSSFRichTextString(prefix);
        appendRich(updated, value.trim(), redFont);
        target.setCellValue(updated);
    }

    private XSSFRichTextString richText(Cell cell) {
        RichTextString value = cell.getRichStringCellValue();
        if (value instanceof XSSFRichTextString) return (XSSFRichTextString) value;
        return new XSSFRichTextString(value == null ? "" : value.getString());
    }

    private XSSFFont fontAtText(XSSFRichTextString rich, String text) {
        if (rich == null || text == null || text.isBlank()) return null;
        int index = rich.getString().indexOf(text);
        return index < 0 ? null : rich.getFontAtIndex(index);
    }

    private void appendRich(XSSFRichTextString rich, String text, XSSFFont font) {
        if (text == null || text.isEmpty()) return;
        if (font == null) rich.append(text);
        else rich.append(text, font);
    }

    private String between(String value, String start, String end) {
        String after = substringAfter(value, start);
        int index = after.indexOf(end);
        return index < 0 ? after.trim() : after.substring(0, index).trim();
    }

    private String substringAfter(String value, String prefix) {
        if (value == null) return "";
        int index = value.indexOf(prefix);
        if (index < 0) return "";
        return value.substring(index + prefix.length()).trim();
    }

    private void fillContactAt(Sheet sheet, int excelNameRow, GmudTemplateRequest.Contact contact) {
        put(sheet, "D" + excelNameRow, contact.name);
        put(sheet, "D" + (excelNameRow + 1), contact.phone);
        put(sheet, "N" + excelNameRow, contact.role);
        put(sheet, "N" + (excelNameRow + 1), contact.email);
    }

    private void fillActivityResponsibles(Sheet sheet, OperatorInfo operator, GmudTemplateRequest.Contact client, Set<String> operatorNames) {
        String clientName = client == null ? "" : nullToEmpty(client.name).trim();
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Cell responsibleCell = cell(sheet, r, 15); // coluna P
            ResponsibleKind kind = responsibleKind(responsibleCell, operatorNames);
            if (kind == ResponsibleKind.OPERATOR) {
                setPlainCellValue(responsibleCell, operator.name());
            } else if (kind == ResponsibleKind.CLIENT && !clientName.isBlank()) {
                setPlainCellValue(responsibleCell, clientName);
            }
        }
    }

    private ResponsibleKind responsibleKind(Cell cell, Set<String> operatorNames) {
        if (cell == null) return ResponsibleKind.STATIC;
        if (cell.getCellType() == CellType.FORMULA) {
            String formula = nullToEmpty(cell.getCellFormula()).replace("$", "").toUpperCase(Locale.ROOT);
            if (formula.matches(".*\\bD28\\b.*")) return ResponsibleKind.OPERATOR;
            if (formula.matches(".*\\bD24\\b.*")) return ResponsibleKind.CLIENT;
        }

        String value = normalize(rawCellText(cell));
        if (value.isBlank()) return ResponsibleKind.STATIC;
        if (operatorNames.contains(value) || value.equals("equipe redix")) return ResponsibleKind.OPERATOR;
        if (value.equals("equipe cliente") || value.equals("cliente")) return ResponsibleKind.CLIENT;
        return ResponsibleKind.STATIC;
    }

    private Set<String> normalizedOperatorNames(List<OperatorInfo> operators) {
        Set<String> result = new HashSet<>();
        for (OperatorInfo operator : operators) result.add(normalize(operator.name()));
        return result;
    }

    private void fillAgenda(Sheet sheet, List<GmudTemplateRequest.AgendaValue> agendaValues) {
        if (agendaValues == null || agendaValues.isEmpty()) return;
        Map<String, GmudTemplateRequest.AgendaValue> requested = new LinkedHashMap<>();
        for (GmudTemplateRequest.AgendaValue item : agendaValues) {
            if (item != null && item.key != null) requested.put(item.key, item);
        }
        if (requested.isEmpty()) return;

        int start = findRowInColumnExact(sheet, 1, "Agenda");
        int end = findNextRowInColumnStartingWith(sheet, start + 1, 1, "Riscos");
        if (start < 0 || end < 0 || end <= start) return;

        Map<String, Integer> occurrences = new HashMap<>();
        for (int r = start + 1; r < end; r++) {
            String label = rawCellText(cell(sheet, r, 1));
            if (label.isBlank()) continue;
            String base = normalize(label);
            int occurrence = occurrences.merge(base, 1, Integer::sum);
            String key = agendaKey(base, occurrence);
            GmudTemplateRequest.AgendaValue requestedValue = requested.get(key);
            if (requestedValue == null) continue;
            writeAgendaValue(cell(sheet, r, 11), label, requestedValue.value);
        }
    }

    private void writeAgendaValue(Cell target, String label, String value) {
        if (target == null) return;
        target.setBlank();
        if (value == null || value.isBlank()) return;

        String kind = agendaKind(target, label, value);
        if (kind.equals("DATE")) {
            LocalDate date = parseDate(value);
            if (date != null) {
                target.setCellValue(java.sql.Date.valueOf(date));
                return;
            }
        }
        if (kind.equals("TIME") || kind.equals("DURATION")) {
            Double fraction = parseTimeFraction(value);
            if (fraction != null) {
                target.setCellValue(fraction);
                return;
            }
        }
        target.setCellValue(value);
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"))) {
            try { return LocalDate.parse(value.trim(), formatter); }
            catch (Exception ignored) { }
        }
        return null;
    }

    private Double parseTimeFraction(String value) {
        String clean = value.trim();
        String[] parts = clean.split(":");
        if (parts.length < 2 || parts.length > 3) return null;
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            if (minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59 || hours < 0) return null;
            return (hours * 3600d + minutes * 60d + seconds) / 86400d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void embedEvidenceImages(XSSFWorkbook workbook, Sheet sheet, TemplateSpec spec,
                                     List<MultipartFile> attachments, List<MultipartFile> prints,
                                     MultipartFile tasyAppManagerPrint, MultipartFile tasyIntegrationPrint, MultipartFile tasyTomcatPrint) throws Exception {
        List<MultipartFile> attachmentFiles = nonEmptyFiles(attachments);
        List<MultipartFile> printFiles = nonEmptyFiles(prints);

        if (isStandardTasy(spec)) {
            embedTasyEvidenceImages(workbook, sheet, attachmentFiles, printFiles,
                    tasyAppManagerPrint, tasyIntegrationPrint, tasyTomcatPrint);
            return;
        }

        if (attachmentFiles.isEmpty() && printFiles.isEmpty()) return;

        int evidenceHeader = findEvidenceHeader(sheet);
        if (evidenceHeader < 0) evidenceHeader = createEvidenceSection(sheet);

        int printHeader = findRowInColumnExact(sheet, 1, "Print de confirmação dos backups");
        if (printHeader > evidenceHeader) {
            if (!attachmentFiles.isEmpty()) {
                int startRow = findEvidenceContentStart(sheet, evidenceHeader, printHeader);
                placeImageGridWithinZone(workbook, sheet, attachmentFiles,
                        startRow, printHeader,
                        GENERIC_EVIDENCE_START_COL, GENERIC_EVIDENCE_END_COL_EXCLUSIVE,
                        "Anexos");
            }

            if (!printFiles.isEmpty()) {
                int boundary = findEvidenceBoundary(sheet, printHeader);
                int startRow = findEvidenceContentStart(sheet, printHeader, boundary);
                placeImageGridWithinZone(workbook, sheet, printFiles,
                        startRow, boundary,
                        GENERIC_EVIDENCE_START_COL, GENERIC_EVIDENCE_END_COL_EXCLUSIVE,
                        "Prints");
            }
            return;
        }

        // Alguns templates possuem somente uma área de Anexos. Nesses casos,
        // anexos e prints compartilham a mesma zona reservada, sem criar linhas abaixo dela.
        List<MultipartFile> combined = new ArrayList<>(attachmentFiles);
        combined.addAll(printFiles);
        int boundary = findEvidenceBoundary(sheet, evidenceHeader);
        int startRow = findEvidenceContentStart(sheet, evidenceHeader, boundary);
        placeImageGridWithinZone(workbook, sheet, combined,
                startRow, boundary,
                GENERIC_EVIDENCE_START_COL, GENERIC_EVIDENCE_END_COL_EXCLUSIVE,
                "Anexos / Prints");
    }

    private void embedTasyEvidenceImages(XSSFWorkbook workbook, Sheet sheet, List<MultipartFile> attachments,
                                         List<MultipartFile> legacyPrints, MultipartFile appManagerPrint,
                                         MultipartFile integrationPrint, MultipartFile tomcatPrint) throws Exception {
        List<MultipartFile> fallback = new ArrayList<>(legacyPrints);
        MultipartFile appManager = chooseEvidenceFile(appManagerPrint, fallback);
        MultipartFile integration = chooseEvidenceFile(integrationPrint, fallback);
        MultipartFile tomcat = chooseEvidenceFile(tomcatPrint, fallback);

        int evidenceHeader = findRowInColumnExact(sheet, 1, "Evidências do Ambiente");
        if (evidenceHeader < 0) evidenceHeader = createEvidenceSection(sheet);
        int descriptionRow = findNextRowInColumnStartingWith(sheet, evidenceHeader + 1, 1, "Evidenciar:");
        if (descriptionRow < 0) descriptionRow = evidenceHeader + 1;

        CellRangeAddress evidenceArea = findMergedRegionContaining(sheet, descriptionRow, 1);
        int pictureStartRow = Math.min(descriptionRow + 3, findEvidenceBoundary(sheet, evidenceHeader) - 1);
        int pictureEndRow = evidenceArea != null
                ? evidenceArea.getLastRow() + 1
                : findEvidenceBoundary(sheet, evidenceHeader);
        int startColumn = evidenceArea != null
                ? evidenceArea.getFirstColumn()
                : GENERIC_EVIDENCE_START_COL;
        int endColumnExclusive = evidenceArea != null
                ? evidenceArea.getLastColumn() + 1
                : GENERIC_EVIDENCE_END_COL_EXCLUSIVE;

        if (pictureEndRow <= pictureStartRow) {
            throw new IllegalArgumentException("A área de Evidências do Ambiente do template Atualização Tasy não possui espaço para imagens.");
        }

        List<MultipartFile> extras = new ArrayList<>(attachments);
        extras.addAll(fallback);

        // Os três prints específicos permanecem em posições fixas, lado a lado.
        // Quando existem anexos adicionais, reservamos a parte inferior da própria área de evidências para eles.
        int dedicatedEndRow = extras.isEmpty()
                ? pictureEndRow
                : pictureStartRow + Math.max(4, ((pictureEndRow - pictureStartRow) * 2) / 3);
        dedicatedEndRow = Math.min(dedicatedEndRow, pictureEndRow);

        int totalColumns = Math.max(3, endColumnExclusive - startColumn);
        int firstEnd = startColumn + totalColumns / 3;
        int secondEnd = startColumn + (totalColumns * 2) / 3;
        placeSingleImageInSlot(workbook, sheet, appManager,
                pictureStartRow, dedicatedEndRow, startColumn, firstEnd, "Acesso ao AppManager");
        placeSingleImageInSlot(workbook, sheet, integration,
                pictureStartRow, dedicatedEndRow, firstEnd, secondEnd, "Integrações");
        placeSingleImageInSlot(workbook, sheet, tomcat,
                pictureStartRow, dedicatedEndRow, secondEnd, endColumnExclusive, "Execução dos Tomcats");

        if (!extras.isEmpty() && dedicatedEndRow < pictureEndRow) {
            placeImageGridWithinZone(workbook, sheet, extras,
                    dedicatedEndRow, pictureEndRow,
                    startColumn, endColumnExclusive,
                    "Anexos adicionais");
        }
    }

    private MultipartFile chooseEvidenceFile(MultipartFile preferred, List<MultipartFile> fallback) {
        if (preferred != null && !preferred.isEmpty()) return preferred;
        if (fallback.isEmpty()) return null;
        return fallback.remove(0);
    }

    private List<MultipartFile> nonEmptyFiles(List<MultipartFile> files) {
        if (files == null) return List.of();
        return files.stream().filter(Objects::nonNull).filter(file -> !file.isEmpty()).toList();
    }

    private int findEvidenceHeader(Sheet sheet) {
        int row = findRowInColumnExact(sheet, 1, "Anexos");
        if (row >= 0) return row;
        return findRowInColumnExact(sheet, 1, "Evidências do Ambiente");
    }

    private int findEvidenceBoundary(Sheet sheet, int sectionStart) {
        int approver = findNextRowInColumnExact(sheet, sectionStart + 1, 1, "Aprovador");
        int escalation = findNextRowInColumnStartingWith(sheet, sectionStart + 1, 1, "Plano de escalonamento");
        if (approver >= 0 && escalation >= 0) return Math.min(approver, escalation);
        if (approver >= 0) return approver;
        if (escalation >= 0) return escalation;
        return sheet.getLastRowNum() + 1;
    }

    private int findEvidenceContentStart(Sheet sheet, int headerRow, int boundaryRow) {
        int row = headerRow + 1;
        // Mantém textos/instruções que já existam logo abaixo do cabeçalho da seção.
        while (row < boundaryRow && !rawCellText(cell(sheet, row, 1)).isBlank()) row++;
        return Math.min(row, Math.max(headerRow + 1, boundaryRow - 1));
    }

    private int createEvidenceSection(Sheet sheet) {
        // Em templates sem área de evidência (ex.: Tasy Emergencial), cria o bloco
        // imediatamente antes de Aprovador, mantendo o Plano de escalonamento acima dele.
        int boundary = findRowInColumnExact(sheet, 1, "Aprovador");
        if (boundary < 0) boundary = findRowInColumnStartingWith(sheet, 1, "Plano de escalonamento");
        if (boundary < 0) boundary = sheet.getLastRowNum() + 1;

        CellStyle headerStyle = null;
        if (boundary <= sheet.getLastRowNum()) {
            Cell source = cell(sheet, boundary, 1);
            if (source != null) headerStyle = source.getCellStyle();
            int rowsToCreate = 2 + CREATED_EVIDENCE_CONTENT_ROWS;
            sheet.shiftRows(boundary, sheet.getLastRowNum(), rowsToCreate, true, false);
        }

        Row headerRow = sheet.getRow(boundary);
        if (headerRow == null) headerRow = sheet.createRow(boundary);
        Cell header = headerRow.getCell(1);
        if (header == null) header = headerRow.createCell(1);
        if (headerStyle != null) header.setCellStyle(headerStyle);
        header.setCellValue("Anexos / Evidências");
        try { sheet.addMergedRegion(new CellRangeAddress(boundary, boundary, 1, 16)); }
        catch (Exception ignored) { }

        Row descriptionRow = sheet.getRow(boundary + 1);
        if (descriptionRow == null) descriptionRow = sheet.createRow(boundary + 1);
        Cell description = descriptionRow.getCell(1);
        if (description == null) description = descriptionRow.createCell(1);
        description.setCellValue("Arquivos enviados pelo formulário da GMUD");
        try { sheet.addMergedRegion(new CellRangeAddress(boundary + 1, boundary + 1, 1, 16)); }
        catch (Exception ignored) { }

        for (int i = 0; i < CREATED_EVIDENCE_CONTENT_ROWS; i++) {
            int rowIndex = boundary + 2 + i;
            Row row = sheet.getRow(rowIndex);
            if (row == null) row = sheet.createRow(rowIndex);
            if (row.getHeight() <= 0) row.setHeight(sheet.getDefaultRowHeight());
        }
        return boundary;
    }

    private CellRangeAddress findMergedRegionContaining(Sheet sheet, int row, int column) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (row >= region.getFirstRow() && row <= region.getLastRow()
                    && column >= region.getFirstColumn() && column <= region.getLastColumn()) {
                return region;
            }
        }
        return null;
    }

    private void placeImageGridWithinZone(XSSFWorkbook workbook, Sheet sheet, List<MultipartFile> files,
                                          int startRow, int endRowExclusive,
                                          int startCol, int endColExclusive,
                                          String context) throws Exception {
        if (files == null || files.isEmpty()) return;
        if (endRowExclusive <= startRow || endColExclusive <= startCol) {
            throw new IllegalArgumentException("A área demarcada de " + context + " não possui espaço para as imagens.");
        }

        int count = files.size();
        int gridColumns = count == 1 ? 1 : (count <= 4 ? 2 : 3);
        gridColumns = Math.min(gridColumns, Math.max(1, endColExclusive - startCol));
        int gridRows = (int) Math.ceil(count / (double) gridColumns);
        int availableRows = endRowExclusive - startRow;
        if (availableRows < gridRows) {
            throw new IllegalArgumentException("Há imagens demais para a área demarcada de " + context + ". Remova alguns arquivos ou reduza a quantidade.");
        }

        for (int i = 0; i < count; i++) {
            int gridRow = i / gridColumns;
            int gridCol = i % gridColumns;
            int slotStartRow = startRow + (availableRows * gridRow) / gridRows;
            int slotEndRow = startRow + (availableRows * (gridRow + 1)) / gridRows;
            int availableCols = endColExclusive - startCol;
            int slotStartCol = startCol + (availableCols * gridCol) / gridColumns;
            int slotEndCol = startCol + (availableCols * (gridCol + 1)) / gridColumns;
            placeSingleImageInSlot(workbook, sheet, files.get(i),
                    slotStartRow, slotEndRow, slotStartCol, slotEndCol, context);
        }
    }

    private void placeSingleImageInSlot(XSSFWorkbook workbook, Sheet sheet, MultipartFile file,
                                        int startRow, int endRowExclusive,
                                        int startCol, int endColExclusive,
                                        String context) throws Exception {
        if (file == null || file.isEmpty()) return;
        if (endRowExclusive <= startRow || endColExclusive <= startCol) return;

        byte[] bytes = file.getBytes();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IllegalArgumentException("Arquivo inválido em " + context + ": " + safeFilename(file));
        }

        double maxWidthPx = Math.max(24d, zoneWidthPixels(sheet, startCol, endColExclusive) - (IMAGE_PADDING_PX * 2));
        double maxHeightPx = Math.max(24d, zoneHeightPixels(sheet, startRow, endRowExclusive) - (IMAGE_PADDING_PX * 2));
        double scale = Math.min(maxWidthPx / image.getWidth(), maxHeightPx / image.getHeight());
        scale = Math.max(0.001d, Math.min(1d, scale * 0.96d));

        int pictureIndex = workbook.addPicture(bytes, pictureType(file));
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(startCol);
        anchor.setRow1(startRow);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        Picture picture = drawing.createPicture(anchor, pictureIndex);
        picture.resize(scale);
    }

    private double zoneWidthPixels(Sheet sheet, int startCol, int endColExclusive) {
        double width = 0d;
        for (int c = startCol; c < endColExclusive; c++) {
            width += Math.max(8d, sheet.getColumnWidth(c) / 256d * 7d);
        }
        return width;
    }

    private double zoneHeightPixels(Sheet sheet, int startRow, int endRowExclusive) {
        double height = 0d;
        for (int r = startRow; r < endRowExclusive; r++) {
            Row row = sheet.getRow(r);
            double points = row == null ? sheet.getDefaultRowHeightInPoints() : row.getHeightInPoints();
            if (points <= 0) points = sheet.getDefaultRowHeightInPoints();
            height += points * 96d / 72d;
        }
        return height;
    }

    private int pictureType(MultipartFile file) {
        String contentType = nullToEmpty(file.getContentType()).toLowerCase(Locale.ROOT);
        String filename = safeFilename(file).toLowerCase(Locale.ROOT);
        if (contentType.equals("image/png") || filename.endsWith(".png")) return Workbook.PICTURE_TYPE_PNG;
        if (contentType.equals("image/jpeg") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return Workbook.PICTURE_TYPE_JPEG;
        throw new IllegalArgumentException("Apenas imagens PNG ou JPG/JPEG são aceitas em Anexos e Prints: " + safeFilename(file));
    }

    private String safeFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "imagem";
        return name.replaceAll("[\\r\\n]", "_");
    }

    private List<RiskInfo> extractRisks(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int header = findRowContaining(sheet, "Riscos Mapeados");
        int end = findRowContaining(sheet, "Atividades do Plano de testes");
        if (header < 0 || end < 0 || end <= header) return List.of();

        List<RiskInfo> result = new ArrayList<>();
        for (int r = header + 1; r < end; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String risk = text(row.getCell(1), formatter, evaluator);
            if (risk.isBlank()) continue;
            result.add(new RiskInfo(
                    risk,
                    text(row.getCell(8), formatter, evaluator),
                    text(row.getCell(11), formatter, evaluator),
                    text(row.getCell(13), formatter, evaluator)
            ));
        }
        return result;
    }

    private List<ActivityInfo> extractActivities(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator,
                                                 List<String> headings, Set<String> operatorNames) {
        int heading = -1;
        for (String candidate : headings) {
            heading = findRowContaining(sheet, candidate);
            if (heading >= 0) break;
        }
        if (heading < 0) return List.of();

        int tableHeader = -1;
        for (int r = heading + 1; r <= Math.min(sheet.getLastRowNum(), heading + 6); r++) {
            String value = text(cell(sheet, r, 1), formatter, evaluator);
            if (normalize(value).equals("atividade")) {
                tableHeader = r;
                break;
            }
        }
        if (tableHeader < 0) return List.of();

        List<ActivityInfo> result = new ArrayList<>();
        for (int r = tableHeader + 1; r <= sheet.getLastRowNum(); r++) {
            String activity = text(cell(sheet, r, 1), formatter, evaluator);
            String normalized = normalize(activity);
            if (isNextSection(normalized)) break;
            if (activity.isBlank()) continue;
            Cell responsibleCell = cell(sheet, r, 15);
            ResponsibleKind kind = responsibleKind(responsibleCell, operatorNames);
            result.add(new ActivityInfo(
                    activity,
                    text(cell(sheet, r, 12), formatter, evaluator),
                    text(responsibleCell, formatter, evaluator),
                    kind.name()
            ));
        }
        return result;
    }

    private RollbackInfo extractRollback(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator, Set<String> operatorNames) {
        String title;
        List<ActivityInfo> items;
        if (findRowContaining(sheet, "Atividades do Plano de volta") >= 0) {
            title = "Plano de volta";
            items = extractActivities(sheet, formatter, evaluator, List.of("Atividades do Plano de volta"), operatorNames);
        } else {
            title = "Plano de correção";
            items = extractActivities(sheet, formatter, evaluator, List.of("Atividades do Plano de correção"), operatorNames);
        }
        return new RollbackInfo(title, items);
    }

    private List<AgendaField> extractAgenda(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int start = findRowInColumnExact(sheet, 1, "Agenda");
        int end = findNextRowInColumnStartingWith(sheet, start + 1, 1, "Riscos");
        if (start < 0 || end < 0 || end <= start) return List.of();

        Map<String, Integer> occurrences = new HashMap<>();
        List<AgendaField> result = new ArrayList<>();
        for (int r = start + 1; r < end; r++) {
            String label = text(cell(sheet, r, 1), formatter, evaluator);
            if (label.isBlank()) continue;
            Cell valueCell = cell(sheet, r, 11);
            String value = text(valueCell, formatter, evaluator);
            String base = normalize(label);
            int occurrence = occurrences.merge(base, 1, Integer::sum);
            result.add(new AgendaField(
                    agendaKey(base, occurrence),
                    label,
                    value,
                    agendaKind(valueCell, label, value)
            ));
        }
        return result;
    }

    private String agendaKey(String normalizedLabel, int occurrence) {
        return normalizedLabel + "#" + occurrence;
    }

    private String agendaKind(Cell valueCell, String label, String formattedValue) {
        String normalizedLabel = normalize(label);
        String value = nullToEmpty(formattedValue).toLowerCase(Locale.ROOT);
        String format = "";
        if (valueCell != null && valueCell.getCellStyle() != null) {
            format = nullToEmpty(valueCell.getCellStyle().getDataFormatString()).toLowerCase(Locale.ROOT);
        }

        if (normalizedLabel.contains("tempo")) return "DURATION";
        if (value.contains(":") || normalizedLabel.contains("hora") || normalizedLabel.contains("horário") || format.contains("h")) return "TIME";
        if (value.contains("/") || normalizedLabel.contains("data") || format.contains("d")) return "DATE";
        return "TEXT";
    }

    private TasyTemplateInfo extractTasyDetails(TemplateSpec spec, Sheet sheet) {
        if (!isStandardTasy(spec)) return null;

        int databaseHeading = findRowInColumnExact(sheet, 1, "Responsável Redix - Time de Banco de Dados");
        ContactInfo databaseContact = new ContactInfo("", "", "", "");
        if (databaseHeading >= 0) {
            int excelNameRow = databaseHeading + 2;
            databaseContact = new ContactInfo(
                    rawCellText(cell(sheet, excelNameRow - 1, 3)),
                    rawCellText(cell(sheet, excelNameRow, 3)),
                    rawCellText(cell(sheet, excelNameRow - 1, 13)),
                    rawCellText(cell(sheet, excelNameRow, 13))
            );
        }

        String stopActivity = rawCellText(findCellInColumnStartingWith(sheet, 1, "Baixar os serviços de integração:"));
        String tiePrefix = "TIE - Parada do serviço do Tasy Interfaces - ";
        String tomcatPrefix = "\nTomcat - Parada dos serviços do Tomcat Integração - ";
        String tie = between(stopActivity, tiePrefix, tomcatPrefix);
        String tomcat = substringAfter(stopActivity, tomcatPrefix);

        String removed = substringAfter(rawCellText(findCellInColumnStartingWith(sheet, 1, "Remover imagens da antiga versão")),
                "Remover imagens da antiga versão");
        String added = substringAfter(rawCellText(findCellInColumnStartingWith(sheet, 1, "Adcionar imagens da nova versão")),
                "Adcionar imagens da nova versão");
        String artifacts = substringAfter(rawCellText(findCellInColumnStartingWith(sheet, 1, "Alterar artefatos nos servidores de integração")),
                "Alterar artefatos nos servidores de integração, contemplando os seguintes artefatos:\n");

        return new TasyTemplateInfo(databaseContact, tie, tomcat, removed, added, artifacts);
    }

    private List<FixedField> extractEnvironmentDetails(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int start = findRowContaining(sheet, "Dados e requisitos do ambiente");
        int end = findRowInColumnExact(sheet, 1, "Agenda");
        if (start < 0 || end < 0 || end <= start) return List.of();
        List<FixedField> result = new ArrayList<>();
        for (int r = start + 1; r < end; r++) {
            String label = text(cell(sheet, r, 1), formatter, evaluator);
            if (label.isBlank() || normalize(label).startsWith("classificação do ambiente")) continue;
            String value = firstNonBlankValue(sheet.getRow(r), 6, 16, formatter, evaluator);
            result.add(new FixedField(label, value));
        }
        return result;
    }

    private String firstNonBlankValue(Row row, int fromCol, int toCol, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return "";
        for (int c = fromCol; c <= toCol; c++) {
            String value = text(row.getCell(c), formatter, evaluator);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private boolean isNextSection(String normalized) {
        if (normalized.isBlank()) return false;
        return normalized.startsWith("atividades do plano de ")
                || normalized.startsWith("atividades do disaster recovery")
                || normalized.startsWith("evidências")
                || normalized.startsWith("anexos")
                || normalized.startsWith("plano de escalonamento");
    }

    private int findRowContaining(Sheet sheet, String expected) {
        String needle = normalize(expected);
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell c : row) {
                if (normalize(rawCellText(c)).contains(needle)) return r;
            }
        }
        return -1;
    }

    private int findRowInColumnExact(Sheet sheet, int column, String expected) {
        return findNextRowInColumnExact(sheet, 0, column, expected);
    }

    private int findNextRowInColumnExact(Sheet sheet, int startRow, int column, String expected) {
        String needle = normalize(expected);
        for (int r = Math.max(0, startRow); r <= sheet.getLastRowNum(); r++) {
            if (normalize(rawCellText(cell(sheet, r, column))).equals(needle)) return r;
        }
        return -1;
    }

    private Cell findCellInColumnStartingWith(Sheet sheet, int column, String expectedPrefix) {
        int row = findRowInColumnStartingWith(sheet, column, expectedPrefix);
        return row < 0 ? null : cell(sheet, row, column);
    }

    private int findRowInColumnStartingWith(Sheet sheet, int column, String expectedPrefix) {
        return findNextRowInColumnStartingWith(sheet, 0, column, expectedPrefix);
    }

    private int findNextRowInColumnStartingWith(Sheet sheet, int startRow, int column, String expectedPrefix) {
        String prefix = normalize(expectedPrefix);
        for (int r = Math.max(0, startRow); r <= sheet.getLastRowNum(); r++) {
            if (normalize(rawCellText(cell(sheet, r, column))).startsWith(prefix)) return r;
        }
        return -1;
    }

    private int findFirstRowStartingWith(Sheet sheet, String expectedPrefix) {
        return findRowInColumnStartingWith(sheet, 1, expectedPrefix);
    }

    private Cell cell(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        return r == null ? null : r.getCell(col);
    }

    private String text(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try {
            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (Exception ignored) {
            return formatter.formatCellValue(cell).trim();
        }
    }

    private String rawCellText(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
        return formatter.formatCellValue(cell).trim();
    }

    private String normalize(String value) {
        return nullToEmpty(value).trim().toLowerCase(Locale.ROOT);
    }

    private void blankCell(Sheet sheet, String ref) {
        Cell target = getOrCreateCell(sheet, ref);
        target.setBlank();
    }

    private void put(Sheet sheet, String ref, String value) {
        Cell target = getOrCreateCell(sheet, ref);
        target.setBlank(); // remove fórmulas antigas antes de definir o valor
        target.setCellValue(value == null || value.isBlank() ? "-" : value);
    }

    private void putDate(Sheet sheet, String ref, String iso) {
        Cell target = getOrCreateCell(sheet, ref);
        target.setBlank();
        if (iso == null || iso.isBlank()) {
            target.setCellValue("-");
            return;
        }
        try {
            LocalDate date = LocalDate.parse(iso);
            target.setCellValue(java.sql.Date.valueOf(date));
        } catch (Exception e) {
            target.setCellValue(iso);
        }
    }

    private void setPlainCellValue(Cell cell, String value) {
        if (cell == null) return;
        cell.setBlank();
        cell.setCellValue(value == null || value.isBlank() ? "-" : value);
    }

    private Cell getOrCreateCell(Sheet sheet, String ref) {
        String col = ref.replaceAll("[0-9]", "");
        int rowNumber = Integer.parseInt(ref.replaceAll("[A-Z]", ""));
        int colNumber = columnToNumber(col);
        Row row = sheet.getRow(rowNumber - 1);
        if (row == null) row = sheet.createRow(rowNumber - 1);
        Cell cell = row.getCell(colNumber);
        if (cell == null) cell = row.createCell(colNumber);
        return cell;
    }

    private int columnToNumber(String letters) {
        int n = 0;
        for (char c : letters.toCharArray()) n = n * 26 + (c - 'A' + 1);
        return n - 1;
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }

    private enum ResponsibleKind { OPERATOR, CLIENT, STATIC }
    private record TemplateSpec(String id, String label, String sheetName) {}

    public record OperatorInfo(Long id, String name, String phone, String role, String email) {}
    public record RiskInfo(String risk, String probability, String impact, String contingency) {}
    public record ActivityInfo(String activity, String detail, String responsible, String responsibleKind) {}
    public record AgendaField(String key, String label, String value, String kind) {}
    public record FixedField(String label, String value) {}
    public record RollbackInfo(String title, List<ActivityInfo> items) {}
    public record ContactInfo(String name, String phone, String role, String email) {}
    public record TasyTemplateInfo(
            ContactInfo databaseContact,
            String tieStopDetails,
            String tomcatIntegrationStopDetails,
            String removedImages,
            String addedImages,
            String integrationArtifacts
    ) {}
    public record TemplateInfo(
            String id,
            String label,
            String sheetName,
            List<RiskInfo> risks,
            List<ActivityInfo> testActivities,
            List<ActivityInfo> executionActivities,
            RollbackInfo rollback,
            List<AgendaField> agenda,
            List<FixedField> environmentDetails,
            TasyTemplateInfo tasyDetails
    ) {}
}
