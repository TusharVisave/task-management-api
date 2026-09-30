package com.taskmanagement.api.service;

import com.taskmanagement.api.dto.AuthResponse;
import com.taskmanagement.api.dto.LoginRequest;
import com.taskmanagement.api.dto.RegistrationRequest;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.repository.UserRepository;
import com.taskmanagement.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public User register(RegistrationRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles() != null ? request.roles() : java.util.Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .build();
    }
}
