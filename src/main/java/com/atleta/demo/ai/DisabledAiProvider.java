package com.atleta.demo.ai;

import org.springframework.stereotype.Component;

/** Safe default. It keeps the product usable until a real provider is explicitly configured. */
@Component
public class DisabledAiProvider implements AiProvider {
    @Override public String name() { return "disabled"; }
    @Override public String generateJson(AiGenerationRequest request) {
        throw new AiProviderUnavailableException("No hay un proveedor de IA configurado");
    }
}
