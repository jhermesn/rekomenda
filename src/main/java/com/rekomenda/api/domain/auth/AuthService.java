package com.rekomenda.api.domain.auth;

import com.rekomenda.api.domain.auth.dto.*;
import com.rekomenda.api.domain.user.User;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.infrastructure.mail.MailService;
import com.rekomenda.api.shared.exception.BusinessException;
import com.rekomenda.api.shared.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final int RESET_TOKEN_EXPIRY_HOURS = 2;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MailService mailService;
    private final String frontendUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            MailService mailService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Nome de usuário já está em uso", HttpStatus.CONFLICT);
        }

        var user = User.builder()
                .nome(request.nome())
                .username(request.username())
                .email(request.email())
                .dataNascimento(request.dataNascimento())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha())
            );
        } catch (BadCredentialsException _) {
            throw new BusinessException("E-mail ou senha incorretos", HttpStatus.UNAUTHORIZED);
        }

        var token = jwtService.generateToken(request.email());
        return LoginResponse.bearer(token);
    }

    @Transactional
    public void recoverPassword(RecoverPasswordRequest request) {
        var user = userRepository.findByEmail(request.email()).orElse(null);

        // Always return success to avoid user enumeration
        if (user == null) return;

        resetTokenRepository.deleteAllByUserId(user.getId());

        var resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID())
                .dataExpiracao(Instant.now().plus(RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .build();

        resetTokenRepository.save(resetToken);

        var resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        mailService.sendPasswordResetEmail(user.getEmail(), user.getNome(), resetLink);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var resetToken = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado", HttpStatus.BAD_REQUEST));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new BusinessException("Token inválido ou expirado", HttpStatus.BAD_REQUEST);
        }

        var user = resetToken.getUser();
        user.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            jwtService.revokeToken(bearerToken.substring(7));
        }
    }
}
