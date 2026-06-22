package dev.keel.engine;

import dev.keel.model.DecompositionResult;

public interface DecompositionEngine {

    DecompositionResult decompose(String requirement);
}
