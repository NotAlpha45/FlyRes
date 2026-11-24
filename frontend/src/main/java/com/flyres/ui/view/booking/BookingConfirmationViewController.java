package com.flyres.ui.view.booking;

import com.flyres.domain.booking.Reservation;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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

    private final ViewContext context;

    public BookingConfirmationViewController(ViewContext context) {
        this.context = context;
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

    @FXML
    private void onViewTrips() {
        Toasts.error(tripsButton, "My trips will be available in FLY-09.");
    }
}