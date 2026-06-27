package dev.keel.export;

import dev.keel.store.DecompositionStorageService;
import dev.keel.store.StoredDecompositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decompositions/{id}/export")
@Tag(name = "Экспорт")
public class PdfExportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExportController.class);

    private final DecompositionStorageService decompositionStorageService;
    private final PdfExportService pdfExportService;

    public PdfExportController(
            DecompositionStorageService decompositionStorageService,
            PdfExportService pdfExportService
    ) {
        this.decompositionStorageService = decompositionStorageService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(description = "Скачать разбор в PDF.")
    @ApiResponse(responseCode = "404", description = "Разбор не найден.")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        LOGGER.info("Exporting decomposition {} to PDF", id);

        StoredDecompositionResponse stored = decompositionStorageService.findById(id);
        byte[] pdf = pdfExportService.exportDecomposition(stored);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("decomposition-" + id + ".pdf")
                        .build()
                        .toString())
                .body(pdf);
    }
}
