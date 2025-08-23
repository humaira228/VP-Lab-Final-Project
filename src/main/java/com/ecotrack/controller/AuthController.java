package com.ecotrack.controller;

import com.ecotrack.dto.AuthRequest;
import com.ecotrack.dto.AuthResponse;
import com.ecotrack.dto.UserRegistrationDto;
import com.ecotrack.model.User;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        if (userRepository.findByEmail(registrationDto.email()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = new User();
        user.setEmail(registrationDto.email());
        user.setPassword(passwordEncoder.encode(registrationDto.password()));
        user.setFirstName(registrationDto.firstName());
        user.setLastName(registrationDto.lastName());

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        return userRepository.findByEmail(authRequest.email())
                .filter(user -> passwordEncoder.matches(authRequest.password(), user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail());
                    String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
                    return ResponseEntity.ok(new AuthResponse(
                            token,
                            refreshToken,
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name()
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        try {
            String email = jwtUtil.validateTokenAndGetEmail(refreshRequest.refreshToken());
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newToken = jwtUtil.generateToken(user.getEmail());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            return ResponseEntity.ok(new AuthResponse(
                    newToken,
                    newRefreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

record RefreshTokenRequest(String refreshToken) {}
