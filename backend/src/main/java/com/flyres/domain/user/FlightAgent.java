package com.flyres.domain.user;

import java.time.LocalDateTime;

/** A flight agent: manages customer profiles and books on behalf of customers. */
public final class FlightAgent extends User {

    FlightAgent(long id, String email, String firstName, String lastName, String phone, LocalDateTime createdAt) {
        super(id, email, firstName, lastName, phone, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.AGENT;
    }
}
