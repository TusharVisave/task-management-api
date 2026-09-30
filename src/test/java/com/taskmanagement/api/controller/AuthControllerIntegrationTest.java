package com.taskmanagement.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.dto.RegistrationRequest;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerNewUser_successful() throws Exception {
        RegistrationRequest request = new RegistrationRequest("alice", "s3cr3t", Set.of("ROLE_USER"));
        // perform registration
        ResultActions result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("alice"));

        // verify password is stored hashed
        User saved = userRepository.findByUsername("alice").orElseThrow();
        assertThat(passwordEncoder.matches("s3cr3t", saved.getPassword()))
                .as("Password should be stored as BCrypt hash")
                .isTrue();
    }

    @Test
    void registerDuplicateUsername_conflict() throws Exception {
        // first registration
        userRepository.save(new User("bob", passwordEncoder.encode("pwd"), Set.of("ROLE_USER")));

        RegistrationRequest duplicate = new RegistrationRequest("bob", "another", Set.of("ROLE_USER"));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }
}
