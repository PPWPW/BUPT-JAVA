package com.jeiqi.controller;

import com.jeiqi.model.User;
import com.jeiqi.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        }

        User user = userOpt.get();
        if (!user.getPasswordHash().equals(hashPassword(password))) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码错误"));
        }

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername()
        ));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getStats(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "totalGames", user.getTotalGames(),
            "wins", user.getWins(),
            "losses", user.getLosses(),
            "draws", user.getDraws(),
            "winRate", user.getWinRate()
        ));
    }

    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}
