package com.flyres.ui.view.booking;

import com.flyres.common.exception.BookingException;
import com.flyres.common.exception.NotFoundException;
import com.flyres.common.exception.ValidationException;
import com.flyres.control.PaymentController;
import com.flyres.control.FlightSearchController;
import com.flyres.domain.booking.BookingRequest;
import com.flyres.domain.booking.PaymentDetails;
import com.flyres.domain.booking.Reservation;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.flight.Seat;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.PaymentForm;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import com.flyres.ui.view.search.FlightDetailsViewController;
import com.flyres.ui.view.search.FlightSearchViewController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Payment form and trip summary for the chosen seat. */
public final class PaymentViewController {

    @FXML
    private Label flightLabel;
    @FXML
    private Label routeLabel;
    @FXML
    private Label departureLabel;
    @FXML
    private Label seatLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private PaymentForm paymentForm;
    @FXML
    private Button payButton;

    private final ViewContext context;
    private Flight flight;
    private Seat seat;
    private SearchCriteria returnTo;
    private boolean bookingInProgress;

    public PaymentViewController(ViewContext context) {
        this.context = context;
    }

    /** Supplies the details and selected seat from the flight-details screen. */
    public void showBooking(Flight flight, Seat seat, SearchCriteria searchToRestore) {
        this.flight = flight;
        this.seat = seat;
        this.returnTo = searchToRestore;
        flightLabel.setText(flight.getFlightNumber() + " · " + flight.airline().name());
        routeLabel.setText(flight.getRoute().origin().code() + " → " + flight.getRoute().destination().code());
        departureLabel.setText(Formats.dateTime(flight.getDepartureTime()));
        seatLabel.setText("Seat " + seat.label());
        totalLabel.setText(Formats.money(flight.getPriceCents()));
        paymentForm.setCardholderName(context.session().requireUser().getFullName());
        payButton.setText("Pay " + Formats.money(flight.getPriceCents()));
    }

    @FXML
    private void onBack() {
        backToDetails();
    }

    @FXML
    private void onPay() {
        if (bookingInProgress) {
            return;
        }
        bookingInProgress = true;
        paymentForm.clearErrors();
        payButton.setDisable(true);
        PaymentDetails details = paymentForm.toDetails();
        boolean booked = false;
        try {
            Reservation reservation = context.backend().booking().book(context.session().requireUser(),
                    new BookingRequest(context.session().requireUser().getId(), flight.getId(), seat.id(),
                            flight.getPriceCents(), details));
            booked = true;
            String digits = details.digits();
            BookingConfirmationViewController confirmation = context.router().goTo(ViewId.BOOKING_CONFIRMATION);
            confirmation.showReservation(reservation, digits.substring(digits.length() - 4));
        } catch (ValidationException e) {
            paymentForm.showErrors(e.getFieldErrors());
        } catch (BookingException e) {
            Toasts.error(payButton, e.getMessage());
            if (PaymentController.DECLINED.equals(e.getMessage())) {
                paymentForm.clearCvv();
            } else if (FlightSearchController.FLIGHT_UNAVAILABLE.equals(e.getMessage())) {
                backToSearch();
            } else {
                backToDetails();
            }
        } catch (NotFoundException e) {
            Toasts.error(payButton, e.getMessage());
            backToSearch();
        } finally {
            if (!booked) {
                bookingInProgress = false;
                payButton.setDisable(false);
            }
        }
    }

    private void backToDetails() {
        FlightDetailsViewController detailsView = context.router().goTo(ViewId.FLIGHT_DETAILS);
        detailsView.showFlight(flight.getId(), returnTo);
    }

    private void backToSearch() {
        FlightSearchViewController search = context.router().goTo(ViewId.FLIGHT_SEARCH);
        if (returnTo != null) {
            search.restore(returnTo);
        }
    }

}