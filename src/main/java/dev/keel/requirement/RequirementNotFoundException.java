package dev.keel.requirement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RequirementNotFoundException extends RuntimeException {

    public RequirementNotFoundException(Long id) {
        super("Требование #" + id + " не найдено.");
    }
}
