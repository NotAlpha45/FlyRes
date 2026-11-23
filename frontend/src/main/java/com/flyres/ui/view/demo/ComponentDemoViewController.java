package com.flyres.ui.view.demo;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.ValidationException;
import com.flyres.domain.booking.Payment;
import com.flyres.ui.component.ConfirmDialog;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.PaymentForm;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import com.flyres.ui.component.ConfirmDialog;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.component.SeatMapView;
import com.flyres.ui.component.Toasts;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
 * Drives the component demo screen. It uses static sample data, so the "required field" checks
 * here stand in for errors that real screens get from a control. The payment form is the exception:
 * it calls the real {@code PaymentController.processPayment}, which validates and authorizes a card
 * but saves nothing.
 */
public class ComponentDemoViewController {

    private static final long DEMO_AMOUNT_CENTS = 18900;

    private final ViewContext context;

    @FXML
    private PaymentForm paymentForm;
    @FXML
    private Button payButton;

    public ComponentDemoViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void onDemoPay() {
        paymentForm.clearErrors();
        try {
            Payment payment = context.backend().payments()
                    .processPayment(context.session().requireUser(), paymentForm.toDetails(), DEMO_AMOUNT_CENTS);
            Toasts.success(payButton, "Approved: " + Formats.money(payment.getAmountCents()) + " on the card ending "
                    + payment.getCardLast4() + ". Demo only, nothing was saved.");
        } catch (ValidationException e) {
            paymentForm.showErrors(e.getFieldErrors());
        } catch (BookingException | AccessDeniedException e) {
            paymentForm.clearCvv();
            Toasts.error(payButton, e.getMessage());
        }
    }

    private record SampleFlight(String number, String route, String departure, String price) {
    }

    private static final ObservableList<SampleFlight> SAMPLE_FLIGHTS = FXCollections.observableArrayList(
            new SampleFlight("AC211", "YYC → YVR", "Tomorrow 07:00", "$189.00"),
            new SampleFlight("WS101", "YYC → YYZ", "In 2 days 08:15", "$329.00"),
            new SampleFlight("F8601", "YYC → YYZ", "In 3 days 06:45", "$219.00"),
            new SampleFlight("AC405", "YYZ → YUL", "In 5 days 11:00", "$169.00"));

    private static final int SAMPLE_ROWS = 6;
    private static final int SAMPLE_SEATS_PER_ROW = 6;

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
    @FXML
    private SeatMapView seatMap;
    @FXML
    private Label seatSelection;

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

        seatMap.setSeats(SAMPLE_ROWS, SAMPLE_SEATS_PER_ROW, sampleSeats());
        seatSelection.setText("Pick a seat");
        seatMap.selectedSeatProperty().addListener((observable, previous, seat) ->
                seatSelection.setText(seat == null ? "Pick a seat" : "Selected " + seat.label()));
    }

    /** A 6 × 6 cabin (3–3) with a few seats already taken. */
    private static List<SeatAvailability> sampleSeats() {
        Set<String> taken = Set.of("1A", "1B", "2C", "3D", "3E", "5F");
        List<SeatAvailability> seats = new ArrayList<>();
        long id = 1;
        for (int row = 1; row <= SAMPLE_ROWS; row++) {
            for (int column = 0; column < SAMPLE_SEATS_PER_ROW; column++) {
                String label = row + String.valueOf((char) ('A' + column));
                seats.add(new SeatAvailability(new Seat(id++, 0, label), taken.contains(label)));
            }
        }
        return seats;
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
