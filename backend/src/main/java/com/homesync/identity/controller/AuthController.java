package com.homesync.identity.controller;

import com.homesync.identity.dto.LoginRequest;
import com.homesync.identity.dto.LoginResponse;
import com.homesync.identity.dto.RegisterRequest;
import com.homesync.identity.dto.UserResponse;
import com.homesync.identity.entity.User;
import com.homesync.identity.service.AuthService;
import com.homesync.identity.service.UserService;
import com.homesync.shared.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.email(),
                request.password(),
                request.displayName());

        UserResponse response = UserResponse.from(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.authenticate(
                request.email(),
                request.password());

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(
                LoginResponse.from(user, token));
    }

}