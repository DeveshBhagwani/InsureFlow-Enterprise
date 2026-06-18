package com.insureflow.enterprise.controller;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and password recovery")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Registration successful"));
    }

    @Operation(summary = "Login to receive JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse result = authService.authenticateUser(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Login successful"));
    }

    @Operation(summary = "Refresh access token using a refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        JwtResponse result = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Token refreshed successfully"));
    }

    @Operation(summary = "Request password reset token")
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        String result = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Reset request processed"));
    }

    @Operation(summary = "Confirm password reset and set new password")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<String>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        String result = authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Password reset successful"));
    }
}
