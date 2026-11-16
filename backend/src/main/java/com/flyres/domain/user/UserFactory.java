package com.flyres.domain.user;

import java.time.LocalDateTime;

/**
 * Factory pattern: the only way to build a {@link User}. Picks {@link Customer}, {@link FlightAgent}
 * or {@link Administrator} from the role, which comes from the {@code users.role} column.
 */
public final class UserFactory {

    private UserFactory() {
    }

    /** Builds the user subclass that matches {@code role}; {@code phone} may be null. */
    public static User create(Role role, long id, String email, String firstName, String lastName,
                              String phone, LocalDateTime createdAt) {
        return switch (role) {
            case CUSTOMER -> new Customer(id, email, firstName, lastName, phone, createdAt);
            case AGENT -> new FlightAgent(id, email, firstName, lastName, phone, createdAt);
            case ADMIN -> new Administrator(id, email, firstName, lastName, phone, createdAt);
        };
    }
}
