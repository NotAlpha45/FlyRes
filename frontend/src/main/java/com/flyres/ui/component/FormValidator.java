package com.flyres.ui.component;

import atlantafx.base.theme.Styles;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

/**
 * Shows validation errors inline, in a label under each field. It only displays errors:
 * the rules themselves live in the controls, which report them with a field name.
 * <p>
 * Usage: register each field with its error label, then call {@link #showError} for each
 * error and {@link #clearErrors()} before the next attempt.
 */
public final class FormValidator {

    private record Field(Control input, Label error) {
    }

    private final Map<String, Field> fields = new LinkedHashMap<>();

    /** Registers a field under {@code name}; typing in it clears its error. */
    public FormValidator field(String name, Control input, Label errorLabel) {
        Field field = new Field(input, errorLabel);
        fields.put(name, field);
        hide(field);
        if (input instanceof TextInputControl text) {
            text.textProperty().addListener((observable, oldValue, newValue) -> hide(field));
        }
        return this;
    }

    /** Shows {@code message} under the field registered as {@code name} and marks the field invalid. */
    public void showError(String name, String message) {
        Field field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("No field registered as " + name);
        }
        field.error().setText(message);
        field.error().setVisible(true);
        field.error().setManaged(true);
        field.input().pseudoClassStateChanged(Styles.STATE_DANGER, true);
    }

    /** Hides every error. */
    public void clearErrors() {
        fields.values().forEach(FormValidator::hide);
    }

    /** Returns true if any field is showing an error. */
    public boolean hasErrors() {
        return fields.values().stream().anyMatch(field -> field.error().isVisible());
    }

    /** Moves keyboard focus to the first field showing an error, if any. */
    public void focusFirstError() {
        fields.values().stream()
                .filter(field -> field.error().isVisible())
                .findFirst()
                .ifPresent(field -> field.input().requestFocus());
    }

    private static void hide(Field field) {
        field.error().setText("");
        field.error().setVisible(false);
        field.error().setManaged(false);
        field.input().pseudoClassStateChanged(Styles.STATE_DANGER, false);
    }
}
