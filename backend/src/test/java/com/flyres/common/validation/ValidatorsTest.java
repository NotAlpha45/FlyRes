package com.flyres.common.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flyres.common.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidatorsTest {

    @ParameterizedTest
    @ValueSource(strings = {"maya@example.com", "a.b+c@sub.domain.ca", "  padded@example.com  "})
    void isEmail_wellFormed_returnsTrue(String email) {
        assertTrue(Validators.isEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"maya", "maya@", "maya@example", "@example.com", "ma ya@example.com"})
    void isEmail_malformed_returnsFalse(String email) {
        assertFalse(Validators.isEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Admin#2026", "abcdefg1", "Pass word 9"})
    void isStrongPassword_letterDigitAndLongEnough_returnsTrue(String password) {
        assertTrue(Validators.isStrongPassword(password));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc12", "abcdefgh", "12345678"})
    void isStrongPassword_tooShortOrMissingLetterOrDigit_returnsFalse(String password) {
        assertFalse(Validators.isStrongPassword(password));
    }

    @Test
    void isStrongPassword_over128Characters_returnsFalse() {
        assertFalse(Validators.isStrongPassword("a1".repeat(65)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"403-555-0142", "+1 (587) 555 0199", "4035550142"})
    void isPhone_wellFormed_returnsTrue(String phone) {
        assertTrue(Validators.isPhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "phone", "403-555-0142 ext 4", "+"})
    void isPhone_malformed_returnsFalse(String phone) {
        assertFalse(Validators.isPhone(phone));
    }

    @Test
    void normalizeEmail_mixedCaseWithSpaces_returnsTrimmedLowerCase() {
        assertEquals("maya@example.com", Validators.normalizeEmail("  Maya@Example.COM "));
    }

    @Test
    void trimToNull_blank_returnsNull() {
        assertNull(Validators.trimToNull("   "));
    }

    @Test
    void throwIfAny_severalErrors_throwsAllInOrderKeepingFirstPerField() {
        ValidationErrors errors = new ValidationErrors()
                .add("b", "first b")
                .add("a", "first a")
                .add("b", "second b");

        ValidationException e = assertThrows(ValidationException.class, errors::throwIfAny);

        assertEquals(List.of("b", "a"), List.copyOf(e.getFieldErrors().keySet()));
        assertEquals("first b", e.getFieldErrors().get("b"));
        assertEquals("b", e.getField());
        assertEquals("first b", e.getMessage());
    }

    @Test
    void throwIfAny_noErrors_doesNotThrow() {
        new ValidationErrors().throwIfAny();
    }
}
