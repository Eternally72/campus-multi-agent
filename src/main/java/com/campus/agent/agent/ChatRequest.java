package com.campus.agent.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(Long sessionId, @NotBlank @Size(max = 4000) String message) {
}
