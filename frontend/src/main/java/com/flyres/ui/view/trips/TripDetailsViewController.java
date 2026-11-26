package com.flyres.ui.view.trips;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationDetails;
import com.flyres.domain.booking.ReservationStatus;
import com.flyres.domain.booking.ReservationSummary;
import com.flyres.domain.flight.Flight;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.component.TripTable;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * &lt;&lt;boundary&gt;&gt; One trip: calls {@code BookingController.getReservation} once and shows the flight,
 * seat, status and payment record. Cancel (FLY-10) and change seat (FLY-11) add actions here.
 */
public class TripDetailsViewController {

    @FXML
    private Label codeLabel;
    @FXML
    private StackPane statusHolder;
    @FXML
    private Label noticeLabel;
    @FXML
    private Label flightLabel;
    @FXML
    private Label routeLabel;
    @FXML
    private Label departsLabel;
    @FXML
    private Label arrivesLabel;
    @FXML
    private Label seatLabel;
    @FXML
    private Label aircraftLabel;
    @FXML
    private Label bookedLabel;
    @FXML
    private HBox actionRow;
    @FXML
    private Label amountLabel;
    @FXML
    private Label cardLabel;
    @FXML
    private Label cardholderLabel;
    @FXML
    private Label paymentStatusLabel;
    @FXML
    private Label paidLabel;
    @FXML
    private VBox refundedItem;
    @FXML
    private Label refundedLabel;
    @FXML
    private Label noPaymentLabel;

    private final ViewContext context;

    public TripDetailsViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        for (Node node : new Node[] {noticeLabel, actionRow, refundedItem, noPaymentLabel}) {
            node.managedProperty().bind(node.visibleProperty());
            node.setVisible(false);
        }
    }

    /** Loads and shows reservation {@code reservationId}; goes back to My trips if it can't be shown. */
    public void showTrip(long reservationId) {
        ReservationDetails details;
        try {
            details = context.backend().booking().getReservation(context.session().requireUser(), reservationId);
        } catch (NotFoundException | AccessDeniedException e) {
            // Toast first: the toast layer outlives the screen change.
            Toasts.error(codeLabel, e.getMessage());
            onBack();
            return;
        }
        render(details);
    }

    @FXML
    private void onBack() {
        context.router().goTo(ViewId.MY_TRIPS);
    }

    private void render(ReservationDetails details) {
        ReservationSummary trip = details.summary();
        Reservation reservation = trip.reservation();
        Flight flight = reservation.getFlight();

        codeLabel.setText(reservation.getConfirmationCode());
        statusHolder.getChildren().setAll(TripTable.statusBadge(trip));
        flightLabel.setText(flight.getFlightNumber() + " · " + flight.airline().name());
        routeLabel.setText(flight.getRoute().origin().code() + " " + flight.getRoute().origin().city()
                + "  →  " + flight.getRoute().destination().code() + " " + flight.getRoute().destination().city());
        departsLabel.setText(Formats.dateTime(flight.getDepartureTime()));
        arrivesLabel.setText(Formats.dateTime(flight.getArrivalTime()));
        seatLabel.setText(reservation.getSeat().label());
        aircraftLabel.setText(flight.getAircraft().model());
        bookedLabel.setText(reservation.getCreatedAt().map(Formats::dateTime).orElse("—"));

        noticeLabel.setVisible(trip.status() == ReservationStatus.CANCELLED);
        if (trip.isFlightRemoved()) {
            noticeLabel.setText("The airline removed this flight, so this trip was cancelled and fully refunded.");
        } else if (trip.status() == ReservationStatus.CANCELLED) {
            noticeLabel.setText("This trip was cancelled on "
                    + reservation.getCancelledAt().map(Formats::dateTime).orElse("an unknown date")
                    + " and fully refunded.");
        }

        amountLabel.setText(Formats.money(reservation.getAmountCents()));
        details.paymentRecord().ifPresentOrElse(this::showPayment, () -> noPaymentLabel.setVisible(true));
    }

    private void showPayment(Payment payment) {
        cardLabel.setText("•••• " + payment.getCardLast4());
        cardholderLabel.setText(payment.getCardholderName());
        paymentStatusLabel.setText(payment.getStatus() == PaymentStatus.REFUNDED ? "Refunded" : "Paid");
        paidLabel.setText(payment.getPaidAt().map(Formats::dateTime).orElse("—"));
        payment.getRefundedAt().ifPresent(at -> {
            refundedLabel.setText(Formats.dateTime(at));
            refundedItem.setVisible(true);
        });
    }
}
