package com.flyres.repository;

import com.flyres.common.security.PasswordHash;
import com.flyres.domain.user.User;

/**
 * A user paired with their stored password hash. Only {@code AuthenticationController} reads it;
 * the hash never travels further up, so the UI cannot see it.
 */
public record UserCredentials(User user, PasswordHash password) {
}
