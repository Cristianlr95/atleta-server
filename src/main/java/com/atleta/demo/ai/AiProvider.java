package com.atleta.demo.ai;

/** Vendor-neutral boundary: adapters may call Vertex AI, another provider, or a local model. */
public interface AiProvider {
    String name();
    String generateJson(AiGenerationRequest request);
}
