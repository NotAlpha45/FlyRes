package com.flyres.ui.view.search;

import com.flyres.common.exception.NotFoundException;
import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightDetails;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.flight.Seat;
import com.flyres.ui.component.Formats;
import com.flyres.ui.component.SeatMapView;
import com.flyres.ui.component.Toasts;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

/**
 * &lt;&lt;boundary&gt;&gt; One flight's details and seat map. Loads them with
 * {@code FlightSearchController.getFlightDetails} and lets the user pick a seat. Continuing to payment
 * arrives with booking (FLY-08).
 */
public class FlightDetailsViewController {

    private static final String CONTINUE_COMING_SOON = "Payment and booking are coming soon.";

    @FXML
    private Label titleLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label routeLabel;
    @FXML
    private Label departsLabel;
    @FXML
    private Label arrivesLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private Label aircraftLabel;
    @FXML
    private Label seatsLeftLabel;
    @FXML
    private Label soldOutLabel;
    @FXML
    private SeatMapView seatMap;
    @FXML
    private Label selectionLabel;
    @FXML
    private StackPane continueHolder;
    @FXML
    private Button continueButton;

    private final ViewContext context;
    private long flightId;
    private SearchCriteria returnTo;
    private FlightDetails details;

    public FlightDetailsViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        soldOutLabel.managedProperty().bind(soldOutLabel.visibleProperty());
        soldOutLabel.setVisible(false);
        // A disabled button receives no mouse events, so the tooltip goes on its container.
        Tooltip.install(continueHolder, new Tooltip(CONTINUE_COMING_SOON));
        seatMap.selectedSeatProperty().addListener((observable, previous, seat) -> showSelection(seat));
        showSelection(null);
    }

    /**
     * Shows flight {@code flightId}. {@code searchToRestore} is the search the user came from, so
     * "Back to results" can show it again; it may be null.
     */
    public void showFlight(long flightId, SearchCriteria searchToRestore) {
        this.flightId = flightId;
        this.returnTo = searchToRestore;
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    @FXML
    private void onBack() {
        FlightSearchViewController search = context.router().goTo(ViewId.FLIGHT_SEARCH);
        if (returnTo != null) {
            search.restore(returnTo);
        }
    }

    private void load() {
        try {
            details = context.backend().flightSearch().getFlightDetails(context.session().requireUser(), flightId);
        } catch (NotFoundException e) {
            // Toast first: the toast layer outlives the screen change.
            Toasts.error(titleLabel, e.getMessage());
            onBack();
            return;
        }
        render();
    }

    private void render() {
        Flight flight = details.flight();
        titleLabel.setText(flight.getFlightNumber() + " · " + flight.airline().name());
        priceLabel.setText(Formats.money(flight.getPriceCents()));
        routeLabel.setText(flight.getRoute().origin().code() + " " + flight.getRoute().origin().city()
                + "  →  " + flight.getRoute().destination().code() + " " + flight.getRoute().destination().city());
        departsLabel.setText(Formats.dateTime(flight.getDepartureTime()));
        arrivesLabel.setText(Formats.dateTime(flight.getArrivalTime()));
        durationLabel.setText(Formats.duration(flight.duration()));
        aircraftLabel.setText(flight.getAircraft().model());
        seatsLeftLabel.setText(details.isSoldOut() ? "Sold out" : String.valueOf(details.availableCount()));

        seatMap.setSeats(details.seatRows(), details.seatsPerRow(), details.seats());
        seatMap.setReadOnly(details.isSoldOut());
        soldOutLabel.setVisible(details.isSoldOut());
        showSelection(seatMap.getSelectedSeat());
    }

    private void showSelection(Seat seat) {
        if (seat == null || details == null) {
            selectionLabel.setText(details != null && details.isSoldOut() ? "" : "Pick a free seat on the map.");
        } else {
            selectionLabel.setText("Seat " + seat.label() + " · " + Formats.money(details.flight().getPriceCents()));
        }
        // Continue stays disabled until booking (FLY-08) wires it, whatever the selection.
        continueButton.setDisable(true);
    }
}
