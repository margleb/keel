package dev.keel.export;

import com.lowagie.text.pdf.BaseFont;
import dev.keel.model.DecompositionResult;
import dev.keel.model.IntegrationRisk;
import dev.keel.model.Size;
import dev.keel.model.Stage;
import dev.keel.model.WorkItem;
import dev.keel.store.StoredDecompositionResponse;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class PdfExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExportService.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"))
            .withZone(DEFAULT_ZONE);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))
            .withZone(DEFAULT_ZONE);
    private static final List<SystemFont> SYSTEM_FONTS = List.of(
            new SystemFont("Arial", List.of(
                    "/usr/share/fonts/truetype/msttcorefonts/Arial.ttf",
                    "/usr/share/fonts/truetype/msttcorefonts/arial.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/Library/Fonts/Arial.ttf",
                    "/System/Library/Fonts/Supplemental/Arial.ttf",
                    "C:\\Windows\\Fonts\\arial.ttf"
            )),
            new SystemFont("Courier New", List.of(
                    "/usr/share/fonts/truetype/msttcorefonts/Courier_New.ttf",
                    "/usr/share/fonts/truetype/msttcorefonts/cour.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
                    "/Library/Fonts/Courier New.ttf",
                    "/System/Library/Fonts/Supplemental/Courier New.ttf",
                    "C:\\Windows\\Fonts\\cour.ttf"
            ))
    );

    public byte[] exportDecomposition(StoredDecompositionResponse stored) {
        LOGGER.info("Building PDF export for decomposition {}", stored.id());

        String html = buildHtml(stored);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            registerSystemFonts(renderer);
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Failed to export decomposition {} to PDF", stored.id(), e);
            throw new IllegalStateException("Failed to export decomposition to PDF", e);
        }
    }

    private String buildHtml(StoredDecompositionResponse stored) {
        DecompositionResult result = stored.result();
        List<Stage> stages = safeList(result == null ? null : result.stages());
        List<IntegrationRisk> risks = safeList(result == null ? null : result.integrationRisks());
        Instant createdAt = stored.createdAt() == null ? Instant.now() : stored.createdAt();
        Instant generatedAt = Instant.now();
        int taskCount = countTasks(stages);

        StringBuilder html = new StringBuilder(16_384);
        html.append("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8" />
                  <style type="text/css">
                    body { font-family: Arial, sans-serif; font-size: 12px;
                           color: #16282A; margin: 40px; }
                    h1 { font-size: 18px; color: #093F3C; margin-bottom: 4px; }
                    .meta { font-size: 11px; color: #8A9A98; margin-bottom: 24px; }
                    .stage { margin-bottom: 24px; }
                    .stage-title { font-size: 14px; font-weight: bold;
                                   color: #093F3C; border-bottom: 2px solid #0E5A57;
                                   padding-bottom: 4px; margin-bottom: 12px; }
                    .task { border: 1px solid #D7DEDB; border-radius: 6px;
                            padding: 12px; margin-bottom: 8px; page-break-inside: avoid; }
                    .task-header { margin-bottom: 6px; }
                    .size-badge { display: inline-block; vertical-align: top;
                                  font-size: 10px; font-weight: bold;
                                  padding: 2px 6px; border-radius: 3px; margin-right: 8px; }
                    .size-S { background: #DCEAE7; color: #093F3C; }
                    .size-M { background: #F6F8F6; color: #56666A; }
                    .size-L { background: #F4E6D4; color: #A85E16; }
                    .task-title { display: inline-block; vertical-align: top;
                                  font-weight: bold; font-size: 12px; width: 88%; }
                    .section-label { font-size: 10px; color: #8A9A98;
                                     text-transform: uppercase; margin: 8px 0 4px; }
                    .description { font-size: 11px; line-height: 1.5; }
                    .list-item { font-size: 11px; margin-bottom: 3px;
                                 padding-left: 12px; }
                    .list-item::before { content: "• "; color: #A85E16; }
                    .ac-item::before { color: #2E7D5B; }
                    .dev-note { font-size: 10px; font-family: "Courier New", monospace;
                                background: #EBF0EE; padding: 2px 6px;
                                border-radius: 3px; margin-bottom: 2px; }
                    .tech-tag { font-size: 9px; font-weight: bold;
                                padding: 1px 4px; border-radius: 2px; margin-right: 3px; }
                    .tag-1c { background: #F4E6D4; color: #A85E16; }
                    .tag-api { background: #DCEAE7; color: #093F3C; }
                    .tag-front { background: #E8F0FE; color: #2952A3; }
                    .risks { background: #FFF8F0; border: 1px solid #F4E6D4;
                             border-radius: 6px; padding: 12px; margin-top: 24px; }
                    .risks-title { font-size: 13px; font-weight: bold;
                                   color: #A85E16; margin-bottom: 8px; }
                    .footer { margin-top: 32px; font-size: 10px; color: #8A9A98;
                              border-top: 1px solid #D7DEDB; padding-top: 8px; }
                  </style>
                </head>
                <body>
                  <h1>Декомпозиция требования</h1>
                  <div class="meta">
                    Keel · """);
        html.append(escape(DATE_FORMATTER.format(createdAt)));
        html.append(" · ");
        html.append(stages.size());
        html.append(" эт. · ");
        html.append(taskCount);
        html.append("""
                 зад.
                  </div>
                """);

        for (int i = 0; i < stages.size(); i++) {
            appendStage(html, stages.get(i), i);
        }

        if (!risks.isEmpty()) {
            html.append("""
                    <div class="risks">
                      <div class="risks-title">Сквозные риски</div>
                    """);
            appendListItems(html, risks.stream().map(this::riskDescription).toList(), "");
            html.append("  </div>\n");
        }

        html.append("""
                  <div class="footer">
                    Сгенерировано Keel · """);
        html.append(escape(TIMESTAMP_FORMATTER.format(generatedAt)));
        html.append("""
                  </div>
                </body>
                </html>
                """);

        return html.toString();
    }

    private void registerSystemFonts(ITextRenderer renderer) {
        for (SystemFont font : SYSTEM_FONTS) {
            registerSystemFont(renderer, font);
        }
    }

    private void registerSystemFont(ITextRenderer renderer, SystemFont font) {
        for (String candidate : font.candidates()) {
            Path path = Path.of(candidate);
            if (!Files.isRegularFile(path)) {
                continue;
            }

            try {
                renderer.getFontResolver().addFont(
                        path.toString(),
                        font.family(),
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED,
                        null
                );
                LOGGER.debug("Registered system font {} from {}", font.family(), path);
                return;
            } catch (Exception e) {
                LOGGER.warn("Failed to register system font {} from {}", font.family(), path, e);
            }
        }

        LOGGER.warn("No system font found for PDF family {}", font.family());
    }

    private void appendStage(StringBuilder html, Stage stage, int index) {
        List<WorkItem> items = safeList(stage == null ? null : stage.items());

        html.append("  <div class=\"stage\">\n");
        html.append("    <div class=\"stage-title\">");
        html.append(twoDigits(index + 1));
        html.append(". ");
        html.append(escape(stage == null ? "" : stage.title()));
        html.append("</div>\n");

        for (WorkItem item : items) {
            appendTask(html, item);
        }

        html.append("  </div>\n");
    }

    private void appendTask(StringBuilder html, WorkItem item) {
        Size size = item == null || item.size() == null ? Size.M : item.size();

        html.append("    <div class=\"task\">\n");
        html.append("      <div class=\"task-header\">\n");
        html.append("        <span class=\"size-badge size-");
        html.append(size);
        html.append("\">");
        html.append(size);
        html.append("</span>\n");
        html.append("        <span class=\"task-title\">");
        html.append(escape(item == null ? "" : item.title()));
        html.append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"section-label\">описание</div>\n");
        html.append("      <div class=\"description\">");
        html.append(escapeMultiline(item == null ? "" : item.description()));
        html.append("</div>\n");

        List<String> considerations = safeList(item == null ? null : item.considerations());
        if (!considerations.isEmpty()) {
            html.append("      <div class=\"section-label\">что учесть</div>\n");
            appendListItems(html, considerations, "");
        }

        List<String> acceptanceCriteria = safeList(item == null ? null : item.acceptanceCriteria());
        if (!acceptanceCriteria.isEmpty()) {
            html.append("      <div class=\"section-label\">критерии приёмки</div>\n");
            appendListItems(html, acceptanceCriteria, " ac-item");
        }

        List<String> devNotes = safeList(item == null ? null : item.devNotes());
        if (!devNotes.isEmpty()) {
            html.append("      <div class=\"section-label\">для разработчика</div>\n");
            for (String devNote : devNotes) {
                html.append("      <div class=\"dev-note\">");
                html.append(escapeMultiline(devNote));
                html.append("</div>\n");
            }
        }

        html.append("    </div>\n");
    }

    private void appendListItems(StringBuilder html, List<String> values, String extraClass) {
        for (String value : values) {
            html.append("      <div class=\"list-item");
            html.append(extraClass);
            html.append("\">");
            html.append(escapeMultiline(value));
            html.append("</div>\n");
        }
    }

    private int countTasks(List<Stage> stages) {
        int count = 0;

        for (Stage stage : stages) {
            count += safeList(stage == null ? null : stage.items()).size();
        }

        return count;
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private String riskDescription(IntegrationRisk risk) {
        return risk == null ? "" : risk.description();
    }

    private String escapeMultiline(String value) {
        return escape(value).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br />");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record SystemFont(String family, List<String> candidates) {
    }
}
