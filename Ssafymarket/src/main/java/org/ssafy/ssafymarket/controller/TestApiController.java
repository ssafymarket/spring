package org.ssafy.ssafymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Server is running");
        return response;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", user.getStudentId());
            map.put("name", user.getName());
            map.put("class", user.getClassName());
            map.put("role", user.getRole().name());
            map.put("passwordHash", user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "...");
            return map;
        }).toList();
    }

    @GetMapping("/fix-passwords")
    public Map<String, String> fixPasswords() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            // password123을 BCrypt로 인코딩
            user.setPassword(passwordEncoder.encode("password123"));
        }
        userRepository.saveAll(users);

        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Fixed " + users.size() + " users");
        return response;
    }
}
