package com.flyres.ui.view.booking;

import com.flyres.domain.booking.Reservation;
import com.flyres.ui.component.Formats;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

/** Displays the booking code and the saved trip/payment summary. */
public final class BookingConfirmationViewController {

    private static final DateTimeFormatter BOOKED_AT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm");

    @FXML
    private Label confirmationCodeLabel;
    @FXML
    private Label flightLabel;
    @FXML
    private Label routeLabel;
    @FXML
    private Label seatLabel;
    @FXML
    private Label amountLabel;
    @FXML
    private Label cardLabel;
    @FXML
    private Label bookedAtLabel;
    @FXML
    private Button tripsButton;
    @FXML
    private StackPane tripsHolder;

    private final ViewContext context;

    public BookingConfirmationViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        // A disabled button receives no mouse events, so the tooltip goes on its container.
        Tooltip.install(tripsHolder, new Tooltip("My trips is coming soon. Your booking is saved."));
    }

    /** Renders the persisted booking and the payment's non-sensitive last four digits. */
    public void showReservation(Reservation reservation, String cardLast4) {
        confirmationCodeLabel.setText(reservation.getConfirmationCode());
        flightLabel.setText(reservation.getFlight().getFlightNumber() + " · "
                + reservation.getFlight().airline().name());
        routeLabel.setText(reservation.getFlight().getRoute().origin().code() + " → "
                + reservation.getFlight().getRoute().destination().code());
        seatLabel.setText("Seat " + reservation.getSeat().label());
        amountLabel.setText(Formats.money(reservation.getAmountCents()));
        cardLabel.setText("Card ending in " + cardLast4);
        bookedAtLabel.setText(reservation.getCreatedAt().orElseThrow().format(BOOKED_AT));
    }

    @FXML
    private void onSearchAgain() {
        context.router().goTo(ViewId.FLIGHT_SEARCH);
    }

    /** Placeholder until My trips (FLY-09) exists; the button stays disabled until then. */
    @FXML
    private void onViewTrips() {
    }
}