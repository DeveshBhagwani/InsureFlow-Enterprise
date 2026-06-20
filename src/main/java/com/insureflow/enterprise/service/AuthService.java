package com.insureflow.enterprise.service;

import com.insureflow.enterprise.dto.*;
import com.insureflow.enterprise.exception.BusinessException;
import com.insureflow.enterprise.exception.ResourceNotFoundException;
import com.insureflow.enterprise.model.*;
import com.insureflow.enterprise.repository.*;
import com.insureflow.enterprise.security.CustomUserDetails;
import com.insureflow.enterprise.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Value("${insureflow.jwt.refreshExpirationMs:604800000}") // Default 7 days
    private Long refreshTokenDurationMs;

    @Value("${insureflow.passwordResetExpirationMs:3600000}") // Default 1 hour
    private Long passwordResetDurationMs;

    @Transactional
    public String registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already in use");
        }

        // Force default role to CUSTOMER for security (Priority 1 Privilege Escalation prevention)
        Role userRole = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(UserRole.CUSTOMER).build()));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(roles)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Successfully registered user: {}", user.getEmail());
        return "User registered successfully";
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String jwt = jwtUtils.generateJwtToken(authentication);

        // Generate and save Refresh Token
        RefreshToken refreshToken = createRefreshToken(userDetails.getId());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .id(userDetails.getId())
                .email(userDetails.getUsername())
                .fullName(userDetails.getFullName())
                .roles(roles)
                .build();
    }

    @Transactional
    public JwtResponse refreshAccessToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateJwtTokenFromUsername(user.getEmail());
                    
                    // We can choose to rotate the refresh token or keep the current one.
                    // Rotation is a more secure production-grade mechanism:
                    refreshTokenRepository.deleteByToken(requestRefreshToken);
                    RefreshToken newRefreshToken = createRefreshToken(user.getId());

                    List<String> roles = user.getRoles().stream()
                            .map(role -> "ROLE_" + role.getName().name())
                            .collect(Collectors.toList());

                    return JwtResponse.builder()
                            .token(token)
                            .refreshToken(newRefreshToken.getToken())
                            .id(user.getId())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .roles(roles)
                            .build();
                })
                .orElseThrow(() -> new BusinessException("Refresh token is not in database!"));
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        // Find user by email. If not found, still return success for security.
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            log.warn("Password reset requested for non-existing email: {}", request.getEmail());
            return "If the email is registered in our system, you will receive a password reset token shortly.";
        }

        // Delete any existing reset token for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Create new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusMillis(passwordResetDurationMs))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Mock email delivery: Log reset event securely without leaking the token
        log.info("Password reset token generated securely for user: {}", user.getEmail());

        return "If the email is registered in our system, you will receive a password reset token shortly.";
    }

    @Transactional
    public String confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid password reset token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete token after successful password change
        passwordResetTokenRepository.delete(resetToken);
        log.info("Password successfully reset for user: {}", user.getEmail());

        return "Password reset completed successfully";
    }

    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete old token if exists
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BusinessException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
