package com.taskmanagement.api.controller;

import com.taskmanagement.api.dto.RegistrationRequest;
import com.taskmanagement.api.dto.UserResponse;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.service.AuthService;
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
    public ResponseEntity<UserResponse> register(@RequestBody RegistrationRequest request) {
        User created = authService.register(request);
        UserResponse response = new UserResponse(created.getId(), created.getUsername(), created.getRoles());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
