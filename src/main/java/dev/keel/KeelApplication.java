package dev.keel;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Keel API",
                version = "1.0",
                description = "API для декомпозиции требований с грунтовкой по коду"
        )
)
@SpringBootApplication
public class KeelApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeelApplication.class, args);
    }
}
