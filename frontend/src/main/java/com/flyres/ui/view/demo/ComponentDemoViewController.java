package com.flyres.ui.view.demo;

import com.flyres.ui.component.ConfirmDialog;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.component.Toasts;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Drives the component demo screen. It uses static sample data and no backend, so the
 * "required field" checks here stand in for errors that real screens get from a control.
 */
public class ComponentDemoViewController {

    private record SampleFlight(String number, String route, String departure, String price) {
    }

    private static final ObservableList<SampleFlight> SAMPLE_FLIGHTS = FXCollections.observableArrayList(
            new SampleFlight("AC211", "YYC → YVR", "Tomorrow 07:00", "$189.00"),
            new SampleFlight("WS101", "YYC → YYZ", "In 2 days 08:15", "$329.00"),
            new SampleFlight("F8601", "YYC → YYZ", "In 3 days 06:45", "$219.00"),
            new SampleFlight("AC405", "YYZ → YUL", "In 5 days 11:00", "$169.00"));

    @FXML
    private TextField nameField;
    @FXML
    private Label nameError;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailError;
    @FXML
    private Button archiveButton;
    @FXML
    private CheckBox emptyToggle;
    @FXML
    private TableView<SampleFlight> flightTable;
    @FXML
    private TableColumn<SampleFlight, String> numberColumn;
    @FXML
    private TableColumn<SampleFlight, String> routeColumn;
    @FXML
    private TableColumn<SampleFlight, String> departureColumn;
    @FXML
    private TableColumn<SampleFlight, String> priceColumn;

    private FormValidator form;

    @FXML
    private void initialize() {
        form = new FormValidator()
                .field("name", nameField, nameError)
                .field("email", emailField, emailError);

        bind(numberColumn, SampleFlight::number);
        bind(routeColumn, SampleFlight::route);
        bind(departureColumn, SampleFlight::departure);
        bind(priceColumn, SampleFlight::price);

        Label emptyState = new Label("No flights to show.");
        emptyState.getStyleClass().add("empty-state");
        flightTable.setPlaceholder(emptyState);
        flightTable.setItems(SAMPLE_FLIGHTS);
    }

    @FXML
    private void onSuccessToast() {
        Toasts.success(flightTable, "Booking confirmed. Confirmation code MK7Q2D.");
    }

    @FXML
    private void onErrorToast() {
        Toasts.error(flightTable, "Payment declined. Nothing was charged.");
    }

    @FXML
    private void onArchive() {
        boolean confirmed = ConfirmDialog.show(archiveButton,
                "Archive flight AC211?",
                "3 reservations will be cancelled and fully refunded.",
                "Archive flight");
        if (confirmed) {
            Toasts.success(archiveButton, "Flight AC211 archived.");
        }
    }

    @FXML
    private void onSave() {
        form.clearErrors();
        if (nameField.getText().isBlank()) {
            form.showError("name", "Full name is required.");
        }
        if (emailField.getText().isBlank()) {
            form.showError("email", "Email is required.");
        }
        if (form.hasErrors()) {
            form.focusFirstError();
        } else {
            Toasts.success(nameField, "Profile saved.");
        }
    }

    @FXML
    private void onToggleEmpty() {
        flightTable.setItems(emptyToggle.isSelected() ? FXCollections.observableArrayList() : SAMPLE_FLIGHTS);
    }

    private static void bind(TableColumn<SampleFlight, String> column, Function<SampleFlight, String> value) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    }
}
