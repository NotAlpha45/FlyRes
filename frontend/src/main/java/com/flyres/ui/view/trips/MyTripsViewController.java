package com.flyres.ui.view.trips;

import com.flyres.domain.booking.MyTrips;
import com.flyres.domain.booking.ReservationSummary;
import com.flyres.ui.component.TripTable;
import com.flyres.ui.navigation.ViewContext;
import com.flyres.ui.navigation.ViewId;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;

/**
 * &lt;&lt;boundary&gt;&gt; My trips: calls {@code BookingController.listMyReservations} once and shows the
 * customer's upcoming, past and cancelled trips in three tabs. A trip opens its details.
 */
public class MyTripsViewController {

    @FXML
    private Label summaryLabel;
    @FXML
    private Tab upcomingTab;
    @FXML
    private Tab pastTab;
    @FXML
    private Tab cancelledTab;
    @FXML
    private TripTable upcomingTable;
    @FXML
    private TripTable pastTable;
    @FXML
    private TripTable cancelledTable;

    private final ViewContext context;

    public MyTripsViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        upcomingTable.setEmptyMessage("No upcoming trips. Use Search flights to book one.");
        pastTable.setEmptyMessage("No past trips yet.");
        cancelledTable.setEmptyMessage("No cancelled trips.");
        for (TripTable table : List.of(upcomingTable, pastTable, cancelledTable)) {
            table.addActionColumn("View", this::openTrip);
        }

        MyTrips trips = context.backend().booking().listMyReservations(context.session().requireUser());
        show(upcomingTab, upcomingTable, "Upcoming", trips.upcoming());
        show(pastTab, pastTable, "Past", trips.past());
        show(cancelledTab, cancelledTable, "Cancelled", trips.cancelled());
        summaryLabel.setText(trips.isEmpty()
                ? "You have no trips yet."
                : trips.upcoming().size() + " upcoming, " + trips.past().size() + " past, "
                        + trips.cancelled().size() + " cancelled.");
    }

    @FXML
    private void onSearchFlights() {
        context.router().goTo(ViewId.FLIGHT_SEARCH);
    }

    private void openTrip(ReservationSummary trip) {
        TripDetailsViewController details = context.router().goTo(ViewId.TRIP_DETAILS);
        details.showTrip(trip.reservation().getId());
    }

    private static void show(Tab tab, TripTable table, String title, List<ReservationSummary> trips) {
        tab.setText(title + " (" + trips.size() + ")");
        table.getItems().setAll(trips);
    }
}
