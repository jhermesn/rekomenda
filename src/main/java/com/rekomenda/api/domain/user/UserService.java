package com.rekomenda.api.domain.user;

import com.rekomenda.api.domain.user.dto.UpdateUserRequest;
import com.rekomenda.api.domain.user.dto.UserResponse;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        return UserResponse.from(findById(userId));
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateUserRequest request) {
        var user = findById(userId);

        if (request.nome() != null) {
            user.setNome(request.nome());
        }

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new BusinessException("Nome de usuário já está em uso", HttpStatus.CONFLICT);
            }
            user.setUsername(request.username());
        }

        if (request.novaSenha() != null) {
            user.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    private User findById(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
