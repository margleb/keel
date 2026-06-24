package dev.keel.controller;

import dev.keel.store.DecompositionNotFoundException;
import dev.keel.requirement.RequirementNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DecompositionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DecompositionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(RequirementNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(RequirementNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("Не удалось прочитать запрос. Проверьте формат данных."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception) {
        LOGGER.error("Operation failed", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(toRussianMessage(exception)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected API error", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Произошла внутренняя ошибка сервера. Подробности смотрите в логах."));
    }

    private String toRussianMessage(IllegalStateException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "Не получилось выполнить операцию. Подробности смотрите в логах.";
        }

        if (message.contains("invalid DecompositionResult JSON")) {
            return "Движок вернул некорректный результат. Проверьте ответ Codex в логах сервера.";
        }

        if (message.contains("keel.codex.command")) {
            return "Команда запуска Codex настроена некорректно. Проверьте параметр keel.codex.command.";
        }

        if (message.contains("keel.codex.repo-path")) {
            return "Путь к репозиторию для Codex настроен некорректно. Проверьте параметр keel.codex.repo-path.";
        }

        if (message.contains("Codex process exited")) {
            return "Codex завершился с ошибкой. Подробности смотрите в логах сервера.";
        }

        if (message.contains("Failed to run Codex command")) {
            return "Не удалось запустить Codex. Проверьте команду запуска и путь к репозиторию.";
        }

        if (message.contains("Codex process was interrupted")) {
            return "Выполнение Codex было прервано. Попробуйте запустить разбор еще раз.";
        }

        if (message.contains("Failed to read Codex process output")) {
            return "Не удалось прочитать ответ Codex. Подробности смотрите в логах сервера.";
        }

        if (message.contains("Failed to serialize decomposition result")) {
            return "Не удалось сохранить результат разбора. Подробности смотрите в логах сервера.";
        }

        if (message.contains("Stored decomposition result JSON is invalid")) {
            return "Сохраненный результат разбора поврежден. Попробуйте создать разбор заново.";
        }

        return "Не получилось выполнить операцию. Подробности смотрите в логах сервера.";
    }
}
