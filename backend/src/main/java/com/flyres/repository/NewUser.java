package com.flyres.repository;

import com.flyres.common.security.PasswordHash;
import com.flyres.domain.user.Role;
import java.time.LocalDateTime;

/** The values of a user that has not been saved yet. {@code phone} may be null. */
public record NewUser(Role role, String email, String firstName, String lastName, String phone,
                      PasswordHash password, LocalDateTime createdAt) {
}
