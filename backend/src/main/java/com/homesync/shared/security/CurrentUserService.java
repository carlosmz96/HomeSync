package com.homesync.shared.security;

import com.homesync.identity.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException(
                    "No hay un usuario autenticado.");
        }

        return user;
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

}