package com.taskmanagement.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record RegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,
        Set<String> roles) {}
