import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/**
 * Hall panel. It combines the hall displays and the hall call buttons: one row per elevator,
 * then the speaker, up and down buttons, and fire alarm button. A pressed button lights up,
 * but only a command turns it off.
 *
 */
public class HallPanel extends VBox {

    // Columns for the arrows and the floor number, shared by the elevator rows and the call buttons
    private static final double ARROW = 90;
    private static final double MIDDLE = 136;
    private static final double SIDE = 90;
    private static final double SIDE_GAP = 39;

    static final double HEADER_HEIGHT = 75;

    private final SoftwareBus bus;
    private final int busId;
    private final int floor;

    private final ElevatorRow[] rows = new ElevatorRow[Pfd.ELEVATORS + 1];
    private final PanelParts.Speaker speaker = new PanelParts.Speaker(38);
    private final PanelParts.RoundButton upButton;
    private final PanelParts.RoundButton downButton;
    private final PanelParts.RoundButton fireButton;
    private final Label fireText = PanelParts.label("FIRE", 19, Color.web("#aaaaaa"));
    private final PauseTransition chimeLight = new PauseTransition(Duration.millis(800));
    private boolean fireOn;

    public HallPanel(SoftwareBus bus, int floor) {
        super(31);
        this.bus = bus;
        this.floor = floor;
        this.busId = bus.register();

        StackPane header = new StackPane(PanelParts.label("Floor " + floor, 33, PanelParts.TEXT));
        header.setPrefSize(377, HEADER_HEIGHT);
        header.setMaxSize(377, HEADER_HEIGHT);
        header.setStyle(PanelParts.box(8));

        VBox rowBox = new VBox(23);
        for (int id = 1; id <= Pfd.ELEVATORS; id++) {
            rows[id] = new ElevatorRow(id);
            rowBox.getChildren().add(rows[id]);
        }

        Line divider = new Line(0, 0, 561, 0);
        divider.setStroke(Color.web("#d6d0c4"));
        divider.setStrokeWidth(2);

        upButton = new PanelParts.RoundButton(45, callArrow(true), this::upButtonPressed);
        downButton = new PanelParts.RoundButton(45, callArrow(false), this::downButtonPressed);
        fireButton = new PanelParts.RoundButton(38, fireText, this::fireAlarmPressed);
        fireButton.setLitColors(PanelParts.RED, Color.web("#c62828"));

        // Up and down buttons line up under the up and down arrows in the rows above
        GridPane bottom = PanelParts.columns(SIDE, SIDE_GAP, ARROW, MIDDLE, ARROW, SIDE_GAP, SIDE);
        bottom.setVgap(14);
        bottom.add(speaker, 0, 0);
        bottom.add(upButton, 2, 0);
        bottom.add(downButton, 4, 0);
        bottom.add(fireButton, 6, 0);
        bottom.add(PanelParts.label("Speaker", 21, PanelParts.MUTED), 0, 1);
        bottom.add(PanelParts.label("Up", 21, PanelParts.MUTED), 2, 1);
        bottom.add(PanelParts.label("Down", 21, PanelParts.MUTED), 4, 1);
        bottom.add(PanelParts.label("Fire alarm", 21, PanelParts.MUTED), 6, 1);

        VBox panel = new VBox(26, rowBox, divider, bottom);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(38, 45, 30, 45));
        panel.setStyle(PanelParts.box(14));

        getChildren().addAll(header, panel);
        setAlignment(Pos.TOP_CENTER);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        chimeLight.setOnFinished(e -> speaker.setSounding(fireOn ? PanelParts.RED : null));

        PanelParts.listen(bus, busId, floor, this::handle,
                Pfd.HALL_SET_UP_LIGHT, Pfd.HALL_SET_DOWN_LIGHT, Pfd.HALL_SET_FIRE_LIGHT, Pfd.HALL_PLAY_CHIME);
        PanelParts.listen(bus, busId, SoftwareBus.ALL, this::handle,
                Pfd.HALL_SET_ELEVATOR_FLOOR, Pfd.HALL_SET_ELEVATOR_DIRECTION, Pfd.HALL_SET_ELEVATOR_OUT_OF_SERVICE);
    }

    public int getFloor() {
        return floor;
    }

    // Commands (controller to panel)

    /** Shows the floor number in that elevator's row. */
    public void setElevatorFloor(int id, int floor) {
        if (validElevator(id) && Pfd.validFloor(floor)) {
            rows[id].floor = floor;
            rows[id].show();
        }
    }

    /** Turns the up or down arrow in that elevator's row blue and leaves the other one gray. */
    public void setElevatorDirection(int id, Pfd.Direction dir) {
        if (validElevator(id)) {
            rows[id].direction = dir;
            rows[id].show();
        }
    }

    /** Shows a dash instead of the floor number in that elevator's row and turns both arrows gray. */
    public void setElevatorOutOfService(int id, boolean on) {
        if (validElevator(id)) {
            rows[id].outOfService = on;
            rows[id].direction = Pfd.Direction.NONE;
            rows[id].show();
        }
    }

    public void setUpLight(boolean on) {
        upButton.setLit(on);
    }

    public void setDownLight(boolean on) {
        downButton.setLit(on);
    }

    /** Turns the fire alarm button red and sounds the fire alarm, or turns both off. */
    public void setFireLight(boolean on) {
        fireOn = on;
        fireButton.setLit(on);
        fireText.setTextFill(on ? Color.WHITE : Color.web("#aaaaaa"));
        speaker.setSounding(on ? PanelParts.RED : null);
        Sound.FIRE_ALARM.set(this, on);
    }

    /** Plays the arrival chime. */
    public void playChime() {
        Sound.chime();
        speaker.setSounding(PanelParts.BLUE);
        chimeLight.playFromStart();
    }

    // Events (panel to controller). A pressed button lights up right away.

    private void upButtonPressed() {
        upButton.setLit(true);
        bus.publish(busId, new SoftwareBus.BusMessage(Pfd.HALL_UP_BUTTON_PRESSED, floor, 0));
    }

    private void downButtonPressed() {
        downButton.setLit(true);
        bus.publish(busId, new SoftwareBus.BusMessage(Pfd.HALL_DOWN_BUTTON_PRESSED, floor, 0));
    }

    private void fireAlarmPressed() {
        setFireLight(true);
        bus.publish(busId, new SoftwareBus.BusMessage(Pfd.HALL_FIRE_ALARM_PRESSED, floor, 0));
    }

    private void handle(SoftwareBus.BusMessage m) {
        switch (m.topic()) {
            case Pfd.HALL_SET_ELEVATOR_FLOOR -> setElevatorFloor(m.subtopic(), m.body());
            case Pfd.HALL_SET_ELEVATOR_DIRECTION -> {
                Pfd.Direction dir = Pfd.decode(Pfd.Direction.class, m.body());
                if (dir != null) {
                    setElevatorDirection(m.subtopic(), dir);
                }
            }
            case Pfd.HALL_SET_ELEVATOR_OUT_OF_SERVICE -> setElevatorOutOfService(m.subtopic(), m.body() != 0);
            case Pfd.HALL_SET_UP_LIGHT -> setUpLight(m.body() != 0);
            case Pfd.HALL_SET_DOWN_LIGHT -> setDownLight(m.body() != 0);
            case Pfd.HALL_SET_FIRE_LIGHT -> setFireLight(m.body() != 0);
            case Pfd.HALL_PLAY_CHIME -> playChime();
            default -> {
            }
        }
    }

    private static boolean validElevator(int id) {
        return id >= 1 && id <= Pfd.ELEVATORS;
    }

    private static Polygon callArrow(boolean up) {
        Polygon arrow = PanelParts.arrow(up, 40);
        arrow.setFill(PanelParts.TEXT);
        return arrow;
    }

    /** One elevator's row: name, up arrow, floor number, down arrow. */
    private static class ElevatorRow extends StackPane {

        private final Label number = PanelParts.label("1", 40, PanelParts.TEXT);
        private final Polygon up = PanelParts.arrow(true, 44);
        private final Polygon down = PanelParts.arrow(false, 44);
        private int floor = 1;
        private Pfd.Direction direction = Pfd.Direction.NONE;
        private boolean outOfService;

        ElevatorRow(int id) {
            Label name = PanelParts.label("Elevator " + id, 19, PanelParts.MUTED);
            StackPane.setAlignment(name, Pos.CENTER_LEFT);
            StackPane.setMargin(name, new Insets(0, 0, 0, 19));

            GridPane arrows = PanelParts.columns(ARROW, MIDDLE, ARROW);
            arrows.addRow(0, up, number, down);
            arrows.getRowConstraints().add(PanelParts.centeredRow());
            arrows.setMaxHeight(USE_PREF_SIZE);

            getChildren().addAll(name, arrows);
            setPrefSize(561, 98);
            setStyle(PanelParts.box(10));
        }

        void show() {
            number.setText(outOfService ? "–" : String.valueOf(floor));
            PanelParts.setLit(up, direction == Pfd.Direction.UP);
            PanelParts.setLit(down, direction == Pfd.Direction.DOWN);
        }
    }
}
