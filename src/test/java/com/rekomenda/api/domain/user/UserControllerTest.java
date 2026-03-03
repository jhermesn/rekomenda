package com.rekomenda.api.domain.user;

import com.rekomenda.api.domain.user.dto.UpdateUserRequest;
import com.rekomenda.api.domain.user.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController controller = new UserController(userService);

    @Test
    @DisplayName("getMe usa o subject do Jwt e delega para UserService.getProfile")
    void getMe_usesJwtSubject() {
        var userId = UUID.randomUUID().toString();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId));

        var expected = new UserResponse(
                UUID.fromString(userId),
                "Nome",
                "username",
                "user@example.com",
                LocalDate.of(2000, 1, 1)
        );

        when(userService.getProfile(userId)).thenReturn(expected);

        var response = controller.getMe(jwt);

        assertEquals(expected, response);
    }

    @Test
    @DisplayName("updateMe delega para UserService.updateProfile com subject do Jwt")
    void updateMe_usesJwtSubject() {
        var userId = UUID.randomUUID().toString();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId));

        var request = new UpdateUserRequest("Novo Nome", "novoUsername", "novaSenha");

        controller.updateMe(jwt, request);

        verify(userService).updateProfile(userId, request);
    }
}

