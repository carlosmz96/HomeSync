package com.homesync.identity.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesync.identity.entity.User;
import com.homesync.identity.entity.UserStatus;
import com.homesync.identity.exception.EmailAlreadyRegisteredException;
import com.homesync.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User user = new User(
                UUID.randomUUID(),
                email.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                displayName.trim(),
                UserStatus.PENDING_VERIFICATION,
                now,
                now);

        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

}