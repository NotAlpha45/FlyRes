package com.flyres.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verify_samePassword_returnsTrue() {
        PasswordHash stored = hasher.hash("Secret#123");

        assertTrue(hasher.verify("Secret#123", stored));
    }

    @Test
    void verify_wrongPassword_returnsFalse() {
        PasswordHash stored = hasher.hash("Secret#123");

        assertFalse(hasher.verify("secret#123", stored));
    }

    @Test
    void hash_samePasswordTwice_usesDifferentSaltsAndHashes() {
        PasswordHash first = hasher.hash("Secret#123");
        PasswordHash second = hasher.hash("Secret#123");

        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
    }

    @Test
    void hash_anyPassword_has256BitKeyAnd16ByteSalt() {
        PasswordHash stored = hasher.hash("Secret#123");

        assertEquals(32, Base64.getDecoder().decode(stored.hash()).length);
        assertEquals(16, Base64.getDecoder().decode(stored.salt()).length);
    }

    @Test
    void verify_seededAdminHash_matchesSeedPassword() {
        // Copied from seed.sql: proves the parameters match the ones the seed was generated with.
        PasswordHash admin = new PasswordHash("0ppbLYONUmsAtv2SWbjJ1+EHMV8Wm5h3pBWpe2zM2aI=", "Ha9g46wURZw/PB8VVmdcaw==");

        assertTrue(hasher.verify("Admin#2026", admin));
    }

    @Test
    void toString_neverRevealsHash() {
        PasswordHash stored = hasher.hash("Secret#123");

        assertFalse(stored.toString().contains(stored.hash()));
        assertFalse(stored.toString().contains(stored.salt()));
    }
}
