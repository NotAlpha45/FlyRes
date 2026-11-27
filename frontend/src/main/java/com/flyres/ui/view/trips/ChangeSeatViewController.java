package com.flyres.ui.view.trips;

import com.flyres.common.exception.AccessDeniedException;
import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationDetails;
import com.flyres.domain.flight.FlightDetails;
import com.flyres.domain.flight.Seat;
import com.flyres.ui.component.ConfirmDialog;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.SeatMapView;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * &lt;&lt;boundary&gt;&gt; Change the seat of an upcoming trip: shows the flight's seat map with the current
 * seat outlined, and moves the trip with one {@code BookingController.changeSeat} call. The fare, payment
 * and confirmation code stay the same.
 */
public class ChangeSeatViewController {

    @FXML
    private Label flightLabel;
    @FXML
    private Label currentSeatLabel;
    @FXML
    private SeatMapView seatMap;
    @FXML
    private Label selectionLabel;
    @FXML
    private Button changeButton;

    private final ViewContext context;
    private Reservation reservation;

    public ChangeSeatViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        seatMap.selectedSeatProperty().addListener((observable, previous, seat) -> showSelection(seat));
    }

    /** Shows the seat map for {@code trip}, the reservation whose seat is being changed. */
    public void showTrip(ReservationDetails trip) {
        this.reservation = trip.summary().reservation();
        flightLabel.setText(reservation.getFlight().getFlightNumber() + " · "
                + reservation.getFlight().getRoute().origin().code() + " → "
                + reservation.getFlight().getRoute().destination().code() + " · "
                + Formats.dateTime(reservation.getFlight().getDepartureTime()));
        currentSeatLabel.setText("Your seat is " + reservation.getSeat().label() + ". Pick a free seat to move to; "
                + "your fare stays " + Formats.money(reservation.getAmountCents()) + ".");
        seatMap.setHighlightedSeat(reservation.getSeat());
        loadSeats();
    }

    @FXML
    private void onBack() {
        TripDetailsViewController details = context.router().goTo(ViewId.TRIP_DETAILS);
        details.showTrip(reservation.getId());
    }

    @FXML
    private void onRefresh() {
        loadSeats();
    }

    /** Confirms the move, then changes the seat with one control call and returns to the trip. */
    @FXML
    private void onChangeSeat() {
        Seat target = seatMap.getSelectedSeat();
        if (target == null) {
            return;
        }
        boolean confirmed = ConfirmDialog.show(changeButton, "Change your seat?",
                "Move from " + reservation.getSeat().label() + " to " + target.label() + " on "
                        + reservation.getFlight().getFlightNumber() + "? Your fare stays "
                        + Formats.money(reservation.getAmountCents()) + " and your confirmation code stays "
                        + reservation.getConfirmationCode() + ".",
                "Change seat");
        if (!confirmed) {
            return;
        }
        try {
            context.backend().booking().changeSeat(context.session().requireUser(), reservation.getId(), target.id());
        } catch (BookingException e) {
            // Seat just taken, or the trip was cancelled/departed meanwhile: say why and refresh the map.
            Toasts.error(changeButton, e.getMessage());
            loadSeats();
            return;
        } catch (NotFoundException | AccessDeniedException e) {
            Toasts.error(changeButton, e.getMessage());
            onBack();
            return;
        }
        Toasts.success(changeButton, "Seat changed to " + target.label() + ".");
        onBack();
    }

    private void loadSeats() {
        FlightDetails details;
        try {
            details = context.backend().flightSearch()
                    .getFlightDetails(context.session().requireUser(), reservation.getFlight().getId());
        } catch (NotFoundException e) {
            // The flight departed or was removed while the screen was open.
            Toasts.error(changeButton, e.getMessage());
            onBack();
            return;
        }
        seatMap.setSeats(details.seatRows(), details.seatsPerRow(), details.seats());
        showSelection(seatMap.getSelectedSeat());
    }

    private void showSelection(Seat seat) {
        changeButton.setDisable(seat == null);
        selectionLabel.setText(seat == null
                ? "Pick a free seat on the map."
                : "Move from " + reservation.getSeat().label() + " to " + seat.label());
    }
}
