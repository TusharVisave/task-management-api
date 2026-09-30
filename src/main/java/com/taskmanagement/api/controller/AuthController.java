package com.taskmanagement.api.controller;

import com.taskmanagement.api.dto.AuthResponse;
import com.taskmanagement.api.dto.LoginRequest;
import com.taskmanagement.api.dto.RegistrationRequest;
import com.taskmanagement.api.dto.UserResponse;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        User created = authService.register(request);
        UserResponse response = new UserResponse(created.getId(), created.getUsername(), created.getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
