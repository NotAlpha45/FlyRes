package com.flyres.domain.user;

import java.time.LocalDateTime;

/** A system administrator: manages aircraft, routes, flights and promotions. */
public final class Administrator extends User {

    Administrator(long id, String email, String firstName, String lastName, String phone, LocalDateTime createdAt) {
        super(id, email, firstName, lastName, phone, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
