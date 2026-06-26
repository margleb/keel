package dev.keel.extract;

import dev.keel.controller.ApiError;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Извлечение текста")
public class TextExtractController {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/gif",
            "image/webp"
    );

    private final TextExtractService textExtractService;

    public TextExtractController(TextExtractService textExtractService) {
        this.textExtractService = textExtractService;
    }

    @PostMapping(value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractText(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return badRequest("Файл изображения пуст.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            return badRequest("Размер изображения не должен превышать 5 МБ.");
        }

        String contentType = normalizedContentType(image);
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            return badRequest("Поддерживаются только изображения JPEG, PNG, GIF и WEBP.");
        }

        return ResponseEntity.ok(new ExtractTextResponse(textExtractService.extractText(image)));
    }

    private ResponseEntity<ApiError> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    private String normalizedContentType(MultipartFile image) {
        if (image.getContentType() == null) {
            return "";
        }

        return image.getContentType().trim().toLowerCase(Locale.ROOT);
    }
}
