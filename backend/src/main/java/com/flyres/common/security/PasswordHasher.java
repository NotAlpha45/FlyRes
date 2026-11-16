package com.flyres.common.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes and verifies passwords with PBKDF2WithHmacSHA512, 210 000 iterations, a 256-bit key and
 * a random 16-byte salt per user, all Base64-encoded. These parameters must match {@code seed.sql}.
 * Plain passwords are never stored or logged.
 */
public final class PasswordHasher {

    static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    static final int ITERATIONS = 210_000;
    static final int KEY_LENGTH_BITS = 256;
    static final int SALT_LENGTH_BYTES = 16;

    private final SecureRandom random;

    public PasswordHasher() {
        this(new SecureRandom());
    }

    public PasswordHasher(SecureRandom random) {
        this.random = Objects.requireNonNull(random);
    }

    /** Hashes {@code password} with a fresh random salt. */
    public PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        Base64.Encoder base64 = Base64.getEncoder();
        return new PasswordHash(base64.encodeToString(derive(password, salt)), base64.encodeToString(salt));
    }

    /** Returns true if {@code password} matches {@code stored}; the comparison takes constant time. */
    public boolean verify(String password, PasswordHash stored) {
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] expected = base64.decode(stored.hash());
        byte[] actual = derive(password, base64.decode(stored.salt()));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String password, byte[] salt) {
        char[] chars = Objects.requireNonNull(password, "password").toCharArray();
        KeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
