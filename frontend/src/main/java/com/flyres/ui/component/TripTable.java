package com.flyres.ui.component;

import com.flyres.domain.booking.Reservation;
import com.flyres.domain.booking.ReservationSummary;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/**
 * Reusable table of a customer's trips ({@link ReservationSummary} rows): confirmation code, flight,
 * route, departure, seat, amount paid and a status badge. Used by My trips now, and later by the agent's
 * customer profile (FLY-12). Rows keep the order they are given, so columns are not sortable.
 */
public final class TripTable extends TableView<ReservationSummary> {

    public TripTable() {
        getStyleClass().addAll("data-table", "trip-table");
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getColumns().addAll(List.of(
                column("Code", 72, s -> s.reservation().getConfirmationCode()),
                column("Flight", 64, s -> s.reservation().getFlight().getFlightNumber()),
                column("Route", 96, s -> route(s.reservation())),
                column("Departs", 144, s -> Formats.dateTime(s.reservation().getFlight().getDepartureTime())),
                column("Seat", 56, s -> s.reservation().getSeat().label()),
                numericColumn("Paid", 80, s -> Formats.money(s.reservation().getAmountCents())),
                statusColumn()));
        setEmptyMessage("No trips to show.");
    }

    /** Shows {@code message} instead of an empty table. */
    public void setEmptyMessage(String message) {
        Label placeholder = new Label(message);
        placeholder.getStyleClass().add("empty-state");
        placeholder.setWrapText(true);
        setPlaceholder(placeholder);
    }

    /** Adds a last column with one button per row, and makes a double-click on a row do the same. */
    public void addActionColumn(String buttonText, Consumer<ReservationSummary> action) {
        TableColumn<ReservationSummary, ReservationSummary> column = new TableColumn<>("");
        column.setMinWidth(80);
        column.setSortable(false);
        column.setReorderable(false);
        column.getStyleClass().add("action-column");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(unused -> new TableCell<>() {
            private final Button button = new Button(buttonText);

            {
                button.getStyleClass().add("small");
                button.setOnAction(event -> action.accept(getItem()));
            }

            @Override
            protected void updateItem(ReservationSummary item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : button);
            }
        });
        getColumns().add(column);
        setRowFactory(table -> {
            TableRow<ReservationSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /** The badge text for a trip: "Confirmed", "Completed", "Cancelled" or "Flight removed". */
    public static String statusText(ReservationSummary trip) {
        if (trip.isFlightRemoved()) {
            return "Flight removed";
        }
        return switch (trip.status()) {
            case PENDING -> "Pending";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    /** The badge style class for a trip, e.g. {@code status-confirmed}. */
    public static String statusStyle(ReservationSummary trip) {
        return "status-" + trip.status().name().toLowerCase();
    }

    /** A status badge label for {@code trip}, styled by {@code flyres.css}. */
    public static Label statusBadge(ReservationSummary trip) {
        Label badge = new Label(statusText(trip));
        badge.getStyleClass().addAll("status-badge", statusStyle(trip));
        return badge;
    }

    private static String route(Reservation reservation) {
        return reservation.getFlight().getRoute().origin().code() + " → "
                + reservation.getFlight().getRoute().destination().code();
    }

    private static TableColumn<ReservationSummary, ReservationSummary> statusColumn() {
        TableColumn<ReservationSummary, ReservationSummary> column = new TableColumn<>("Status");
        column.setMinWidth(120);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(unused -> new TableCell<>() {
            @Override
            protected void updateItem(ReservationSummary item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : statusBadge(item));
            }
        });
        return column;
    }

    private static TableColumn<ReservationSummary, String> column(String title, double minWidth,
                                                                  Function<ReservationSummary, String> text) {
        TableColumn<ReservationSummary, String> column = new TableColumn<>(title);
        column.setMinWidth(minWidth);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return column;
    }

    private static TableColumn<ReservationSummary, String> numericColumn(String title, double minWidth,
                                                                         Function<ReservationSummary, String> text) {
        TableColumn<ReservationSummary, String> column = column(title, minWidth, text);
        column.getStyleClass().add("numeric");
        return column;
    }
}
