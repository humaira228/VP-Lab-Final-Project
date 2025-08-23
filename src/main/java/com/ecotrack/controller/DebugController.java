package com.ecotrack.controller;

import com.ecotrack.security.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final JwtUtil jwtUtil;

    public DebugController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/token")
    public String debugToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "No token provided";
        }

        String token = authHeader.substring(7);
        try {
            String email = jwtUtil.validateTokenAndGetEmail(token);
            return "Token is valid for email: " + email;
        } catch (Exception e) {
            return "Token validation failed: " + e.getMessage();
        }
    }
}