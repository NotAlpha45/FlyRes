package com.flyres.ui.component;

import static com.flyres.control.strategy.CardPayment.CARDHOLDER_NAME;
import static com.flyres.control.strategy.CardPayment.CARD_NUMBER;
import static com.flyres.control.strategy.CardPayment.CVV;
import static com.flyres.control.strategy.CardPayment.EXPIRY;

import com.flyres.domain.booking.PaymentDetails;
import java.time.Year;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.IntStream;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Reusable card payment form: cardholder name, card number (grouped in fours as typed), expiry month
 * and year, and CVV (masked, 3 digits). It only collects input; {@code PaymentController} validates it.
 * Fields are registered with a {@link FormValidator} under the {@code CardPayment} field names, so the
 * control's errors show under the right field via {@link #showErrors}.
 */
public final class PaymentForm extends VBox {

    private static final int MAX_DIGITS = 19;
    private static final int YEARS_AHEAD = 12;

    private final TextField nameField = new TextField();
    private final TextField numberField = new TextField();
    private final ComboBox<Integer> monthBox = new ComboBox<>();
    private final ComboBox<Integer> yearBox = new ComboBox<>();
    private final PasswordField cvvField = new PasswordField();
    private final FormValidator form;

    private boolean reformatting;

    public PaymentForm() {
        getStyleClass().add("payment-form");

        nameField.setPromptText("Name as it appears on the card");
        numberField.setPromptText("1234 5678 9012 3456");
        cvvField.setPromptText("123");

        // Digits, spaces and dashes only, at most 19 digits; regrouped in fours as the user types.
        numberField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9 \\-]*")
                        && change.getControlNewText().replaceAll("\\D", "").length() <= MAX_DIGITS ? change : null));
        numberField.textProperty().addListener((observable, previous, text) -> regroup(text));
        cvvField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9]{0,3}") ? change : null));

        monthBox.getItems().setAll(IntStream.rangeClosed(1, 12).boxed().toList());
        monthBox.setPromptText("MM");
        monthBox.setConverter(twoDigits());
        // Cosmetic range only: the control decides whether the card has expired.
        int thisYear = Year.now().getValue();
        yearBox.getItems().setAll(IntStream.rangeClosed(thisYear, thisYear + YEARS_AHEAD).boxed().toList());
        yearBox.setPromptText("YYYY");

        Label nameError = errorLabel();
        Label numberError = errorLabel();
        Label expiryError = errorLabel();
        Label cvvError = errorLabel();

        HBox expiryInputs = new HBox(monthBox, yearBox);
        expiryInputs.getStyleClass().add("expiry-inputs");
        VBox expiry = field("Expiry", expiryInputs, expiryError);
        VBox cvv = field("Security code (CVV)", cvvField, cvvError);
        HBox.setHgrow(expiry, Priority.ALWAYS);
        HBox.setHgrow(cvv, Priority.ALWAYS);
        HBox lastRow = new HBox(expiry, cvv);
        lastRow.getStyleClass().add("form-row");

        getChildren().addAll(
                field("Cardholder name", nameField, nameError),
                field("Card number", numberField, numberError),
                lastRow);

        form = new FormValidator()
                .field(CARDHOLDER_NAME, nameField, nameError)
                .field(CARD_NUMBER, numberField, numberError)
                .field(EXPIRY, monthBox, expiryError)
                .field(CVV, cvvField, cvvError);
    }

    /** The details as typed. Expiry is null until both month and year are chosen. */
    public PaymentDetails toDetails() {
        YearMonth expiry = monthBox.getValue() == null || yearBox.getValue() == null
                ? null
                : YearMonth.of(yearBox.getValue(), monthBox.getValue());
        return new PaymentDetails(nameField.getText(), numberField.getText(), expiry, cvvField.getText());
    }

    /** Shows each error under its field and focuses the first one. */
    public void showErrors(Map<String, String> fieldErrors) {
        fieldErrors.forEach(form::showError);
        form.focusFirstError();
    }

    public void clearErrors() {
        form.clearErrors();
    }

    /** Clears the CVV, e.g. after a decline, so it is typed again and not kept on screen. */
    public void clearCvv() {
        cvvField.clear();
    }

    /** Pre-fills the cardholder name, e.g. with the customer's name. */
    public void setCardholderName(String name) {
        nameField.setText(name);
    }

    private void regroup(String text) {
        if (reformatting || text == null) {
            return;
        }
        String digits = text.replaceAll("\\D", "");
        String grouped = digits.replaceAll("(\\d{4})(?=\\d)", "$1 ");
        if (!grouped.equals(text)) {
            reformatting = true;
            numberField.setText(grouped);
            numberField.positionCaret(grouped.length());
            reformatting = false;
        }
    }

    private static VBox field(String label, javafx.scene.Node input, Label error) {
        Label caption = new Label(label);
        caption.getStyleClass().add("field-label");
        VBox box = new VBox(caption, input, error);
        box.getStyleClass().add("form-field");
        return box;
    }

    private static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setWrapText(true);
        return label;
    }

    private static StringConverter<Integer> twoDigits() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer month) {
                return month == null ? "" : String.format("%02d", month);
            }

            @Override
            public Integer fromString(String text) {
                return text == null || text.isBlank() ? null : Integer.valueOf(text.strip());
            }
        };
    }
}
