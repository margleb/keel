package dev.keel.store;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DecompositionNotFoundException extends RuntimeException {

    public DecompositionNotFoundException(Long id) {
        super("Разбор #" + id + " не найден.");
    }
}
