package com.flyres.ui.view.trips;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.domain.booking.Payment;
import com.flyres.domain.booking.PaymentStatus;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationDetails;
import com.flyres.domain.booking.ReservationStatus;
import com.flyres.domain.booking.ReservationSummary;
import com.flyres.domain.flight.Flight;
import com.flyres.ui.component.ConfirmDialog;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.component.TripTable;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * &lt;&lt;boundary&gt;&gt; One trip: calls {@code BookingController.getReservation} once and shows the flight,
 * seat, status and payment record. Upcoming trips can be cancelled with a full refund or moved to another
 * seat (Change seat).
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

    @FXML
    private Button cancelButton;

    private final ViewContext context;
    private ReservationDetails shown;

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

    @FXML
    private void onChangeSeat() {
        ChangeSeatViewController changeSeat = context.router().goTo(ViewId.CHANGE_SEAT);
        changeSeat.showTrip(shown);
    }

    /** Asks for confirmation, stating the exact refund, then cancels the trip with one control call. */
    @FXML
    private void onCancelTrip() {
        Reservation reservation = shown.summary().reservation();
        Flight flight = reservation.getFlight();
        String refund = Formats.money(reservation.getAmountCents());
        String card = shown.paymentRecord().map(p -> " to the card ending " + p.getCardLast4()).orElse("");
        boolean confirmed = ConfirmDialog.show(cancelButton, "Cancel this trip?",
                "Cancel " + flight.getFlightNumber() + " " + flight.getRoute().origin().code() + " → "
                        + flight.getRoute().destination().code() + " on " + Formats.dateTime(flight.getDepartureTime())
                        + "? " + refund + " will be refunded in full" + card + ".",
                "Cancel trip");
        if (!confirmed) {
            return;
        }
        try {
            ReservationDetails cancelled = context.backend().booking()
                    .cancelReservation(context.session().requireUser(), reservation.getId());
            render(cancelled);
            Toasts.success(codeLabel, "Trip cancelled. " + refund + " refunded.");
        } catch (BookingException e) {
            // Stale screen (already cancelled, or departed meanwhile): say why and show the trip as it is now.
            Toasts.error(codeLabel, e.getMessage());
            showTrip(reservation.getId());
        } catch (NotFoundException | AccessDeniedException e) {
            Toasts.error(codeLabel, e.getMessage());
            onBack();
        }
    }

    private void render(ReservationDetails details) {
        this.shown = details;
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

        // Actions only for upcoming trips; the control refuses the others anyway.
        actionRow.setVisible(trip.status() == ReservationStatus.CONFIRMED);

        amountLabel.setText(Formats.money(reservation.getAmountCents()));
        noPaymentLabel.setVisible(details.paymentRecord().isEmpty());
        refundedItem.setVisible(false);
        details.paymentRecord().ifPresent(this::showPayment);
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
