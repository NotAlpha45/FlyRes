package com.flyres.ui.component;

import com.flyres.domain.flight.Flight;
import com.flyres.domain.flight.FlightSummary;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

/**
 * Reusable table of {@link FlightSummary} rows: flight, airline, route, departure, arrival, duration,
 * price and seats left (column "Seats"). Used by flight search now, and later by schedules and agent booking.
 * Rows keep the order they are given (sorted by departure by the control), so columns are not sortable.
 */
public final class FlightTable extends TableView<FlightSummary> {

    public FlightTable() {
        getStyleClass().addAll("data-table", "flight-table");
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().addAll(List.of(
                column("Flight", 64, s -> s.flight().getFlightNumber()),
                column("Airline", 104, s -> s.flight().airline().name()),
                column("Route", 96, s -> route(s.flight())),
                column("Departs", 144, s -> Formats.dateTime(s.flight().getDepartureTime())),
                column("Arrives", 64, s -> Formats.arrivalTime(s.flight().getDepartureTime(), s.flight().getArrivalTime())),
                column("Duration", 72, s -> Formats.duration(s.flight().duration())),
                numericColumn("Price", 80, s -> Formats.money(s.flight().getPriceCents())),
                numericColumn("Seats", 72, s -> s.availableSeats() == 0 ? "Sold out" : String.valueOf(s.availableSeats()))));
        setEmptyMessage("No flights to show.");
    }

    /** Shows {@code message} instead of an empty table. */
    public void setEmptyMessage(String message) {
        Label placeholder = new Label(message);
        placeholder.getStyleClass().add("empty-state");
        placeholder.setWrapText(true);
        setPlaceholder(placeholder);
    }

    /**
     * Adds a last column with one button per row. When {@code disabledReason} is not null the buttons
     * are disabled and hovering them shows the reason.
     */
    public void addActionColumn(String buttonText, Consumer<FlightSummary> action, String disabledReason) {
        TableColumn<FlightSummary, FlightSummary> column = new TableColumn<>("");
        column.setMinWidth(80);
        column.setSortable(false);
        column.setReorderable(false);
        column.getStyleClass().add("action-column");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(unused -> new TableCell<>() {
            private final Button button = new Button(buttonText);
            private final StackPane holder = new StackPane(button);

            {
                button.getStyleClass().add("small");
                button.setDisable(disabledReason != null);
                // A disabled control receives no mouse events, so the tooltip goes on its container.
                if (disabledReason != null) {
                    Tooltip.install(holder, new Tooltip(disabledReason));
                }
                button.setOnAction(event -> action.accept(getItem()));
            }

            @Override
            protected void updateItem(FlightSummary item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : holder);
            }
        });
        getColumns().add(column);
    }

    private static String route(Flight flight) {
        return flight.getRoute().origin().code() + " → " + flight.getRoute().destination().code();
    }

    private static TableColumn<FlightSummary, String> column(String title, double minWidth,
                                                             Function<FlightSummary, String> text) {
        TableColumn<FlightSummary, String> column = new TableColumn<>(title);
        column.setMinWidth(minWidth);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return column;
    }

    private static TableColumn<FlightSummary, String> numericColumn(String title, double minWidth,
                                                                    Function<FlightSummary, String> text) {
        TableColumn<FlightSummary, String> column = column(title, minWidth, text);
        column.getStyleClass().add("numeric");
        return column;
    }
}
