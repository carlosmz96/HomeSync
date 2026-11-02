package com.homesync.identity.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesync.identity.entity.User;
import com.homesync.identity.entity.UserStatus;
import com.homesync.identity.exception.AccountNotActiveException;
import com.homesync.identity.exception.InvalidCredentialsException;
import com.homesync.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmailNormalized(normalizedEmail)
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }

        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException();
    }

}