package com.rekomenda.api.domain.auth;

import com.rekomenda.api.domain.auth.dto.LoginRequest;
import com.rekomenda.api.domain.auth.dto.LoginResponse;
import com.rekomenda.api.domain.auth.dto.RecoverPasswordRequest;
import com.rekomenda.api.domain.auth.dto.RegisterRequest;
import com.rekomenda.api.domain.auth.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Criar conta")
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login — retorna JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoga o token JWT")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recover-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(summary = "Solicitar e-mail de recuperação de senha")
    public void recoverPassword(@Valid @RequestBody RecoverPasswordRequest request) {
        authService.recoverPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Redefinir senha com token de recuperação")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
