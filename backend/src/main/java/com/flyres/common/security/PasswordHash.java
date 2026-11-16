package com.flyres.common.security;

/**
 * A stored password: the Base64 PBKDF2 hash and the Base64 salt it was made with.
 * {@link #toString()} is redacted so a hash never ends up in a log.
 */
public record PasswordHash(String hash, String salt) {

    @Override
    public String toString() {
        return "PasswordHash[redacted]";
    }
}
