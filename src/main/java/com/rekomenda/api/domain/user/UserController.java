package com.rekomenda.api.domain.user;

import com.rekomenda.api.domain.user.dto.UpdateUserRequest;
import com.rekomenda.api.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil do usuário autenticado")
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(jwt.getSubject());
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza nome, username ou senha")
    public UserResponse updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateProfile(jwt.getSubject(), request);
    }
}
