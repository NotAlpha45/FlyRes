package com.flyres.ui.view.search;

import static com.flyres.control.FlightSearchController.AIRLINE;
import static com.flyres.control.FlightSearchController.DATE;
import static com.flyres.control.FlightSearchController.DESTINATION;
import static com.flyres.control.FlightSearchController.ORIGIN;

import com.flyres.common.exception.ValidationException;
import com.flyres.control.FlightSearchController;
import com.flyres.domain.flight.Airline;
import com.flyres.domain.flight.Airport;
import com.flyres.domain.flight.FlightSummary;
import com.flyres.domain.flight.SearchCriteria;
import com.flyres.domain.user.User;
import com.flyres.ui.component.FlightTable;
import com.flyres.ui.component.FormValidator;
import com.flyres.ui.component.Formats;
import com.flyres.ui.navigation.ViewContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

/**
 * &lt;&lt;boundary&gt;&gt; Flight search for customers and agents. Reads the form, calls
 * {@link FlightSearchController#searchFlights} once and shows the results or the field errors.
 */
public class FlightSearchViewController {

    /** The "All airlines" choice; its blank code means "no airline filter" to the control. */
    private static final Airline ALL_AIRLINES = new Airline(0, "", "All airlines");

    private static final String START_MESSAGE = "Choose where you are flying from and to, then search.";
    private static final String SELECT_COMING_SOON = "Seat selection and booking are coming soon.";

    @FXML
    private ComboBox<Airport> originBox;
    @FXML
    private Label originError;
    @FXML
    private ComboBox<Airport> destinationBox;
    @FXML
    private Label destinationError;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label dateError;
    @FXML
    private ComboBox<Airline> airlineBox;
    @FXML
    private Label airlineError;
    @FXML
    private Label resultsTitle;
    @FXML
    private FlightTable resultsTable;

    private final ViewContext context;
    private FormValidator form;

    public FlightSearchViewController(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        form = new FormValidator()
                .field(ORIGIN, originBox, originError)
                .field(DESTINATION, destinationBox, destinationError)
                .field(DATE, datePicker, dateError)
                .field(AIRLINE, airlineBox, airlineError);

        User user = context.session().requireUser();
        FlightSearchController search = context.backend().flightSearch();

        List<Airport> airports = search.listAirports(user);
        originBox.getItems().setAll(airports);
        destinationBox.getItems().setAll(airports);
        originBox.setConverter(new AirportConverter());
        destinationBox.setConverter(new AirportConverter());

        List<Airline> airlines = new ArrayList<>();
        airlines.add(ALL_AIRLINES);
        airlines.addAll(search.listAirlines(user));
        airlineBox.getItems().setAll(airlines);
        airlineBox.setConverter(new AirlineConverter());
        airlineBox.setValue(ALL_AIRLINES);

        datePicker.setEditable(false);
        // Cosmetic only: the control rejects past dates whatever the picker allows.
        LocalDate today = LocalDate.now();
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today));
            }
        });

        resultsTable.setEmptyMessage(START_MESSAGE);
        resultsTable.addActionColumn("Select", summary -> { }, SELECT_COMING_SOON);
    }

    @FXML
    private void onSearch() {
        form.clearErrors();
        SearchCriteria criteria = new SearchCriteria(
                code(originBox.getValue()),
                code(destinationBox.getValue()),
                datePicker.getValue(),
                airlineBox.getValue() == null ? null : airlineBox.getValue().code());
        try {
            List<FlightSummary> results = context.backend().flightSearch()
                    .searchFlights(context.session().requireUser(), criteria);
            showResults(results);
        } catch (ValidationException e) {
            e.getFieldErrors().forEach(form::showError);
            form.focusFirstError();
        }
    }

    @FXML
    private void onSwap() {
        Airport origin = originBox.getValue();
        originBox.setValue(destinationBox.getValue());
        destinationBox.setValue(origin);
    }

    @FXML
    private void onClearDate() {
        datePicker.setValue(null);
    }

    private void showResults(List<FlightSummary> results) {
        String route = describeSearch();
        resultsTable.getItems().setAll(results);
        if (results.isEmpty()) {
            resultsTitle.setText("Results");
            resultsTable.setEmptyMessage("No flights found for " + route + ".");
        } else {
            resultsTitle.setText(results.size() + (results.size() == 1 ? " flight" : " flights") + " · " + route);
        }
    }

    /** "YYC → YVR on Tue 24 Nov 2026 with WestJet", from the form as it was searched. */
    private String describeSearch() {
        StringBuilder text = new StringBuilder()
                .append(originBox.getValue().code()).append(" → ").append(destinationBox.getValue().code());
        if (datePicker.getValue() != null) {
            text.append(" on ").append(Formats.date(datePicker.getValue()));
        }
        Airline airline = airlineBox.getValue();
        if (airline != null && !airline.code().isEmpty()) {
            text.append(" with ").append(airline.name());
        }
        return text.toString();
    }

    private static String code(Airport airport) {
        return airport == null ? null : airport.code();
    }

    /** Shows an airport as "YYC · Calgary". */
    private static final class AirportConverter extends StringConverter<Airport> {
        @Override
        public String toString(Airport airport) {
            return airport == null ? "" : airport.code() + " · " + airport.city();
        }

        @Override
        public Airport fromString(String text) {
            throw new UnsupportedOperationException("The airport picker is not editable");
        }
    }

    /** Shows an airline by name. */
    private static final class AirlineConverter extends StringConverter<Airline> {
        @Override
        public String toString(Airline airline) {
            return airline == null ? "" : airline.name();
        }

        @Override
        public Airline fromString(String text) {
            throw new UnsupportedOperationException("The airline picker is not editable");
        }
    }
}
