package com.ecotrack.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role
) {}
