package com.rekomenda.api.domain.auth;

import com.rekomenda.api.domain.auth.dto.LoginRequest;
import com.rekomenda.api.domain.auth.dto.LoginResponse;
import com.rekomenda.api.domain.auth.dto.RecoverPasswordRequest;
import com.rekomenda.api.domain.auth.dto.RegisterRequest;
import com.rekomenda.api.domain.auth.dto.ResetPasswordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController controller = new AuthController(authService);

    @Test
    @DisplayName("register delega corretamente para AuthService")
    void register_delegatesToService() {
        var request = new RegisterRequest(
                "Nome",
                "username",
                "user@example.com",
                LocalDate.of(2000, 1, 1),
                "senhaSegura123"
        );

        controller.register(request);

        verify(authService).register(request);
    }

    @Test
    @DisplayName("login retorna LoginResponse do AuthService")
    void login_returnsServiceResponse() {
        var request = new LoginRequest("user@example.com", "senhaSegura123");
        var expected = LoginResponse.bearer("jwt-token");
        when(authService.login(any(LoginRequest.class))).thenReturn(expected);

        var response = controller.login(request);

        assertEquals(expected, response);
    }

    @Test
    @DisplayName("logout delega para AuthService com header recebido")
    void logout_delegatesToService() {
        var header = "Bearer token";

        controller.logout(header);

        verify(authService).logout(header);
    }

    @Test
    @DisplayName("recoverPassword delega para AuthService")
    void recoverPassword_delegatesToService() {
        var request = new RecoverPasswordRequest("user@example.com");

        controller.recoverPassword(request);

        verify(authService).recoverPassword(request);
    }

    @Test
    @DisplayName("resetPassword delega para AuthService")
    void resetPassword_delegatesToService() {
        var request = new ResetPasswordRequest(UUID.randomUUID(), "novaSenha123");

        controller.resetPassword(request);

        verify(authService).resetPassword(request);
    }
}

