package com.flyres.ui.component;

import com.flyres.domain.flight.Seat;
import com.flyres.domain.flight.SeatAvailability;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Reusable seat map: a grid of {@code rows × seatsPerRow} seats with row numbers, seat letters and an
 * aisle after the middle seat ({@code floor(seatsPerRow / 2)}: 2–3, 3–3, 2–2). Taken seats are disabled.
 * At most one seat is selected at a time ({@link #selectedSeatProperty()}). Used by flight details now,
 * and later by change seat (with {@link #setHighlightedSeat}) and passenger lists (read-only).
 */
public final class SeatMapView extends VBox {

    private static final double VIEWPORT_HEIGHT = 360;

    private final GridPane grid = new GridPane();
    private final ToggleGroup group = new ToggleGroup();
    private final ReadOnlyObjectWrapper<Seat> selectedSeat = new ReadOnlyObjectWrapper<>();
    private final Map<Long, ToggleButton> buttons = new HashMap<>();
    private final HBox legend = new HBox();
    private final Region yourSeatLegend;

    private boolean readOnly;
    private Seat highlightedSeat;

    public SeatMapView() {
        getStyleClass().add("seat-map");
        grid.getStyleClass().add("seat-grid");
        grid.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("seat-map-scroll");
        scroll.setFitToWidth(true);
        // Small cabins show in full; large ones scroll after VIEWPORT_HEIGHT.
        scroll.prefViewportHeightProperty().bind(Bindings.min(grid.heightProperty(), VIEWPORT_HEIGHT));

        legend.getStyleClass().add("seat-legend");
        yourSeatLegend = legendItem("Your seat", "your-seat");
        yourSeatLegend.setVisible(false);
        yourSeatLegend.setManaged(false);
        legend.getChildren().addAll(
                legendItem("Available", "available"),
                legendItem("Selected", "selected"),
                legendItem("Taken", "taken"),
                yourSeatLegend);

        getChildren().addAll(legend, scroll);

        group.selectedToggleProperty().addListener((observable, previous, current) ->
                selectedSeat.set(current == null ? null : (Seat) current.getUserData()));
    }

    /**
     * Draws the seats. A previously selected seat stays selected if it is still free, so refreshing
     * the map keeps the user's choice when possible.
     */
    public void setSeats(int seatRows, int seatsPerRow, List<SeatAvailability> seats) {
        Seat previous = selectedSeat.get();
        group.selectToggle(null);
        group.getToggles().clear();
        grid.getChildren().clear();
        buttons.clear();

        int aisle = seatsPerRow >= 2 ? seatsPerRow / 2 : Integer.MAX_VALUE;
        for (int column = 0; column < seatsPerRow; column++) {
            Label letter = new Label(String.valueOf((char) ('A' + column)));
            letter.getStyleClass().add("seat-column-label");
            grid.add(letter, gridColumn(column, aisle), 0);
        }
        if (aisle < seatsPerRow) {
            Region gap = new Region();
            gap.getStyleClass().add("seat-aisle");
            grid.add(gap, aisle + 1, 0);
        }
        for (int row = 1; row <= seatRows; row++) {
            Label number = new Label(String.valueOf(row));
            number.getStyleClass().add("seat-row-label");
            grid.add(number, 0, row);
        }

        for (SeatAvailability availability : seats) {
            Seat seat = availability.seat();
            ToggleButton button = seatButton(seat, availability.taken());
            buttons.put(seat.id(), button);
            grid.add(button, gridColumn(seat.column(), aisle), seat.row());
        }

        applyHighlight();
        if (previous != null && buttons.containsKey(previous.id()) && !buttons.get(previous.id()).isDisabled()) {
            group.selectToggle(buttons.get(previous.id()));
        }
    }

    /** The selected seat, or null. */
    public ReadOnlyObjectProperty<Seat> selectedSeatProperty() {
        return selectedSeat.getReadOnlyProperty();
    }

    public Seat getSelectedSeat() {
        return selectedSeat.get();
    }

    public void clearSelection() {
        group.selectToggle(null);
    }

    /** Marks {@code seat} as the user's current seat (outlined, and listed in the legend). Null clears it. */
    public void setHighlightedSeat(Seat seat) {
        this.highlightedSeat = seat;
        yourSeatLegend.setVisible(seat != null);
        yourSeatLegend.setManaged(seat != null);
        applyHighlight();
    }

    /** A read-only map shows availability but cannot be selected. */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (readOnly) {
            clearSelection();
        }
        grid.setMouseTransparent(readOnly);
        buttons.values().forEach(button -> button.setFocusTraversable(!readOnly && !button.isDisabled()));
    }

    private ToggleButton seatButton(Seat seat, boolean taken) {
        ToggleButton button = new ToggleButton(String.valueOf(seat.letter()));
        button.getStyleClass().add("seat");
        button.setUserData(seat);
        button.setToggleGroup(group);
        button.setFocusTraversable(!readOnly && !taken);
        if (taken) {
            button.getStyleClass().add("taken");
            button.setDisable(true);
            button.setAccessibleText("Seat " + seat.label() + ", taken");
        } else {
            button.setTooltip(new Tooltip("Seat " + seat.label()));
            button.setAccessibleText("Seat " + seat.label() + ", available");
        }
        return button;
    }

    private void applyHighlight() {
        buttons.values().forEach(button -> button.getStyleClass().remove("your-seat"));
        if (highlightedSeat != null && buttons.containsKey(highlightedSeat.id())) {
            buttons.get(highlightedSeat.id()).getStyleClass().add("your-seat");
        }
    }

    /** Grid column for a seat column: column 0 holds the row numbers, and one column is the aisle. */
    private static int gridColumn(int seatColumn, int aisle) {
        return 1 + seatColumn + (seatColumn >= aisle ? 1 : 0);
    }

    private static HBox legendItem(String text, String kind) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("seat-swatch", kind);
        HBox item = new HBox(swatch, new Label(text));
        item.getStyleClass().add("seat-legend-item");
        return item;
    }
}
