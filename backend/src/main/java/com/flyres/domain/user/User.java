package com.flyres.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A person who can log in. The concrete subclass fixes the {@link Role}; instances are built by
 * {@link UserFactory}. A user never carries its password hash: that stays in the data layer.
 */
public abstract sealed class User permits Customer, FlightAgent, Administrator {

    private final long id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final LocalDateTime createdAt;

    User(long id, String email, String firstName, String lastName, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email");
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.phone = phone;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public abstract Role getRole();

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** True if this user has the given role. */
    public boolean hasRole(Role role) {
        return getRole() == role;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && user.id == id && user.getRole() == getRole();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", email=" + email + "]";
    }
}
