package com.jeiqi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeiqi.model.User;
import com.jeiqi.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", "testuser",
            "password", "pass123"
        ));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        User user = new User();
        user.setUsername("existing");
        user.setPasswordHash("hash");
        userRepository.save(user);

        String body = objectMapper.writeValueAsString(Map.of(
            "username", "existing",
            "password", "pass123"
        ));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginWithCorrectPassword() throws Exception {
        User user = new User();
        user.setUsername("loginuser");
        user.setPasswordHash(Integer.toHexString("pass123".hashCode()));
        userRepository.save(user);

        String body = objectMapper.writeValueAsString(Map.of(
            "username", "loginuser",
            "password", "pass123"
        ));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        User user = new User();
        user.setUsername("loginuser");
        user.setPasswordHash(Integer.toHexString("correct".hashCode()));
        userRepository.save(user);

        String body = objectMapper.writeValueAsString(Map.of(
            "username", "loginuser",
            "password", "wrongpass"
        ));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetUserStats() throws Exception {
        User user = new User();
        user.setUsername("statsuser");
        user.setPasswordHash("hash");
        user.setWins(5);
        user.setLosses(3);
        user.setDraws(2);
        user.setTotalGames(10);
        userRepository.save(user);
        String userId = user.getId();

        mockMvc.perform(get("/api/users/" + userId + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("statsuser"))
            .andExpect(jsonPath("$.wins").value(5))
            .andExpect(jsonPath("$.losses").value(3))
            .andExpect(jsonPath("$.draws").value(2))
            .andExpect(jsonPath("$.totalGames").value(10))
            .andExpect(jsonPath("$.winRate").value(0.5));
    }

    @Test
    void shouldReturn404ForNonexistentUserStats() throws Exception {
        mockMvc.perform(get("/api/users/nonexistent/stats"))
            .andExpect(status().isNotFound());
    }
}
