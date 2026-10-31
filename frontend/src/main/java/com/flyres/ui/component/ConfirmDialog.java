package com.flyres.ui.component;

import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

/**
 * Asks the user to confirm a destructive action. The message must say what will happen,
 * e.g. "3 reservations will be cancelled and fully refunded." Cancel is the default button.
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    /** Shows the dialog over the window of {@code owner}; returns true if the user confirmed. */
    public static boolean show(Node owner, String title, String message, String confirmLabel) {
        ButtonType confirm = new ButtonType(confirmLabel, ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, ButtonType.CANCEL);
        alert.initOwner(owner.getScene().getWindow());
        alert.setTitle(title);
        alert.setHeaderText(title);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().setAll(owner.getScene().getStylesheets());
        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.getStyleClass().add(Styles.DANGER);
        confirmButton.setDefaultButton(false);
        ((Button) pane.lookupButton(ButtonType.CANCEL)).setDefaultButton(true);

        return alert.showAndWait().filter(confirm::equals).isPresent();
    }
}
