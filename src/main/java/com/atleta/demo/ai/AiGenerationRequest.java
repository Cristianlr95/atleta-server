package com.atleta.demo.ai;

import java.util.Map;

public record AiGenerationRequest(String promptVersion, String systemInstruction, Map<String, Object> facts) { }
