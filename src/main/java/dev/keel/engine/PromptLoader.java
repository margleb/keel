package dev.keel.engine;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptLoader.class);
    private static final String DECOMPOSITION_PROMPT_PATH = "prompts/decomposition.txt";
    private static final String REPO_CONTEXT_PLACEHOLDER = "{{REPO_CONTEXT}}";
    private static final String REQUIREMENT_PLACEHOLDER = "{{REQUIREMENT}}";

    private String decompositionPromptTemplate;

    @PostConstruct
    public void load() {
        ClassPathResource resource = new ClassPathResource(DECOMPOSITION_PROMPT_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("Required prompt resource not found: " + DECOMPOSITION_PROMPT_PATH);
        }

        try {
            decompositionPromptTemplate = resource.getContentAsString(StandardCharsets.UTF_8);
            LOGGER.info("Loaded decomposition prompt resource: {}", DECOMPOSITION_PROMPT_PATH);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt resource: " + DECOMPOSITION_PROMPT_PATH, e);
        }
    }

    public String buildPrompt(String requirement, String repoContext) {
        String template = decompositionPromptTemplate;
        if (template == null || template.isBlank()) {
            throw new IllegalStateException("Decomposition prompt template is not loaded");
        }

        return template
                .replace(REPO_CONTEXT_PLACEHOLDER, repoContextBlock(repoContext))
                .replace(REQUIREMENT_PLACEHOLDER, requirement == null ? "" : requirement);
    }

    private String repoContextBlock(String repoContext) {
        if (repoContext == null || repoContext.isBlank()) {
            return "";
        }

        return repoContext.trim() + "\n\n";
    }
}
