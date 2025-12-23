package com.jobchain.controller;

import com.jobchain.dto.JwtResponse;
import com.jobchain.dto.LoginRequest;
import com.jobchain.dto.SignupRequest;
import com.jobchain.dto.SignupResponse;
import com.jobchain.entity.UserEntity;
import com.jobchain.entity.enums.Role;
import com.jobchain.repository.UserRepository;
import com.jobchain.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new SignupResponse("Username already exists"));
        }

        // Create STUDENT user ONLY
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT) // 🔒 FIXED ROLE
                .build();

        userRepository.save(user);

        return ResponseEntity
                .status(201)
                .body(new SignupResponse("Student registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return ResponseEntity.ok(new JwtResponse(token));
    }
}

