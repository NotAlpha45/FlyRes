package com.flyres.control;

import com.flyres.repository.ReservationRepository;
import java.util.Objects;
import java.util.Random;

/** Creates six-character booking codes, retrying collisions against persisted reservations. */
final class ConfirmationCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final Random random;

    ConfirmationCodeGenerator(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    String next(ReservationRepository reservations) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int digit = 0; digit < CODE_LENGTH; digit++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!reservations.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique confirmation code");
    }
}