package com.flyres.domain.user;

/** The three user roles, stored as-is in the {@code users.role} column. */
public enum Role {

    CUSTOMER("Customer"),
    AGENT("Flight agent"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
