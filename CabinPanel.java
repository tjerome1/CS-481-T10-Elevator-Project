import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Cabin panel.
 *
 * The panel only talks to the software bus. Button presses and key turns are published as
 * events. A pressed button lights up right away, but only a command turns it off.
 * The display and buzzer change only when a command arrives.
 * It never decides anything on its own.
 */
public class CabinPanel extends VBox {

    
    private static final double COLUMN = 120;
    private static final double MIDDLE = 82;
    private static final double KEY_ANGLE = 35;

    private final SoftwareBus bus;
    private final int busId;
    private final int elevator;

    private final Label floorLabel = PanelParts.label("1", 40, PanelParts.TEXT);
    private final Label messageLabel = PanelParts.label("", 22, PanelParts.TEXT);
    private final Polygon upArrow = PanelParts.arrow(true, 30);
    private final Polygon downArrow = PanelParts.arrow(false, 30);
    private final PanelParts.RoundButton[] buttons = new PanelParts.RoundButton[Pfd.FLOORS + 1];
    private final PanelParts.Speaker speaker = new PanelParts.Speaker(44);
    private final Rectangle keySlot = new Rectangle(6, 35);
    private final Sound.Loop buzzer = new Sound.Loop(Sound.BUZZER_FILE);

    private int floor = 1;
    private Pfd.Message message = Pfd.Message.NONE;
    private Pfd.KeyPosition key = Pfd.KeyPosition.NORMAL;

    public CabinPanel(SoftwareBus bus, int elevator) {
        super(22);
        this.bus = bus;
        this.elevator = elevator;
        this.busId = bus.register();

        Label title = PanelParts.label("Elevator " + elevator, 22, PanelParts.MUTED);

        // Display: up arrow, floor number, down arrow, and the message line under them
        GridPane floorRow = PanelParts.columns(COLUMN, MIDDLE, COLUMN);
        floorRow.addRow(0, upArrow, floorLabel, downArrow);
        floorRow.getRowConstraints().add(PanelParts.centeredRow());
        messageLabel.setMinHeight(28);
        VBox display = new VBox(4, floorRow, messageLabel);
        display.setAlignment(Pos.CENTER);
        display.setPadding(new Insets(14));
        display.setStyle(PanelParts.box(10));

        // Floor buttons: 1 to 5 up the left column and 6 to 10 up the right
        GridPane buttonGrid = PanelParts.columns(COLUMN, MIDDLE, COLUMN);
        buttonGrid.setVgap(27);
        for (int row = 0; row < 5; row++) {
            int left = 5 - row;
            buttonGrid.add(floorButton(left), 0, row);
            buttonGrid.add(floorButton(left + 5), 2, row);
        }

        Line divider = new Line(0, 0, 558, 0);
        divider.setStroke(Color.web("#d6d0c4"));
        divider.setStrokeWidth(2);

        GridPane bottom = PanelParts.columns(COLUMN, MIDDLE, COLUMN);
        bottom.setVgap(10);
        bottom.add(speaker, 0, 0);
        bottom.add(keySwitch(44), 2, 0);
        bottom.add(PanelParts.label("Speaker", 22, PanelParts.MUTED), 0, 1);
        bottom.add(PanelParts.label("Key: Normal | Manual", 22, PanelParts.MUTED), 2, 1);

        getChildren().addAll(title, display, buttonGrid, divider, bottom);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(30, 45, 34, 45));
        setStyle(PanelParts.box(14));
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        PanelParts.listen(bus, busId, elevator, this::handle,
                Pfd.CABIN_SET_FLOOR, Pfd.CABIN_SET_DIRECTION, Pfd.CABIN_SET_MESSAGE,
                Pfd.CABIN_SET_BUTTON_LIGHT, Pfd.CABIN_CLEAR_BUTTON_LIGHTS, Pfd.CABIN_SET_BUZZER);
    }

    public int getElevator() {
        return elevator;
    }

    // Commands (controller to panel)

    /** Shows the floor number on the display. */
    public void setFloor(int floor) {
        if (Pfd.validFloor(floor)) {
            this.floor = floor;
            showFloor();
        }
    }

    /** Turns the up or down arrow blue and leaves the other one gray. NONE leaves both gray. */
    public void setDirection(Pfd.Direction dir) {
        PanelParts.setLit(upArrow, dir == Pfd.Direction.UP);
        PanelParts.setLit(downArrow, dir == Pfd.Direction.DOWN);
    }

    /** Shows a message under the floor number. OFF also replaces the floor number with a dash. */
    public void setMessage(Pfd.Message msg) {
        message = msg;
        messageLabel.setText(msg == Pfd.Message.NONE ? "" : msg.name());
        messageLabel.setTextFill(switch (msg) {
            case MANUAL -> PanelParts.BLUE;
            case OVERLOAD, FIRE -> PanelParts.RED;
            default -> Color.web("#555555");
        });
        showFloor();
    }

    /** Turns the light for one floor button on or off. */
    public void setButtonLight(int floor, boolean on) {
        if (Pfd.validFloor(floor)) {
            buttons[floor].setLit(on);
        }
    }

    /** Turns off every floor button light. */
    public void clearButtonLights() {
        for (int f = 1; f <= Pfd.FLOORS; f++) {
            buttons[f].setLit(false);
        }
    }

    /** Turns the buzzer alarm on or off. */
    public void setBuzzer(boolean on) {
        buzzer.set(this, on);
        speaker.setSounding(on ? PanelParts.RED : null);
    }

    // Events (panel to controller). A pressed button lights up right away.

    private void floorButtonPressed(int floor) {
        buttons[floor].setLit(true);
        bus.publish(busId, new BusMessage(Pfd.CABIN_FLOOR_BUTTON_PRESSED, elevator, floor));
    }

    private void keySwitchMode(Pfd.KeyPosition mode) {
        bus.publish(busId, new BusMessage(Pfd.CABIN_KEY_SWITCH, elevator, mode.ordinal()));
    }

    private void handle(BusMessage m) {
        switch (m.topic()) {
            case Pfd.CABIN_SET_FLOOR -> setFloor(m.body());
            case Pfd.CABIN_SET_DIRECTION -> {
                Pfd.Direction dir = Pfd.decode(Pfd.Direction.class, m.body());
                if (dir != null) {
                    setDirection(dir);
                }
            }
            case Pfd.CABIN_SET_MESSAGE -> {
                Pfd.Message msg = Pfd.decode(Pfd.Message.class, m.body());
                if (msg != null) {
                    setMessage(msg);
                }
            }
            case Pfd.CABIN_SET_BUTTON_LIGHT -> setButtonLight(Math.abs(m.body()), m.body() > 0);
            case Pfd.CABIN_CLEAR_BUTTON_LIGHTS -> clearButtonLights();
            case Pfd.CABIN_SET_BUZZER -> setBuzzer(m.body() != 0);
            default -> {
            }
        }
    }

    private void showFloor() {
        floorLabel.setText(message == Pfd.Message.OFF ? "–" : String.valueOf(floor));
    }

    private Node floorButton(int floor) {
        Label number = PanelParts.label(String.valueOf(floor), 34, PanelParts.TEXT);
        buttons[floor] = new PanelParts.RoundButton(43, number, () -> floorButtonPressed(floor));
        return buttons[floor];
    }

    /** Key cylinder marked N (normal) and M (manual). Clicking it turns the key to the other side. */
    private Node keySwitch(double radius) {
        Circle bezel = new Circle(radius);
        bezel.setFill(Color.web("#f4f4f4"));
        bezel.setStroke(PanelParts.BORDER);
        bezel.setStrokeWidth(2);

        Circle cylinder = new Circle(radius * 0.62);
        cylinder.setFill(Color.web("#d8d8d8"));
        cylinder.setStroke(Color.web("#9a9a9a"));
        cylinder.setStrokeWidth(1.5);

        keySlot.setArcWidth(6);
        keySlot.setArcHeight(6);
        keySlot.setFill(Color.web("#2a2a2a"));
        keySlot.setRotate(-KEY_ANGLE);

        Label normal = PanelParts.label("N", 13, Color.web("#c0392b"));
        normal.setTranslateX(-radius * 0.55);
        normal.setTranslateY(-radius * 0.7);
        Label manual = PanelParts.label("M", 13, Color.web("#8e44ad"));
        manual.setTranslateX(radius * 0.55);
        manual.setTranslateY(-radius * 0.7);

        StackPane keySwitch = new StackPane(bezel, cylinder, keySlot, normal, manual);
        keySwitch.setCursor(Cursor.HAND);
        keySwitch.setOnMousePressed(e -> {
            key = key == Pfd.KeyPosition.NORMAL ? Pfd.KeyPosition.MANUAL : Pfd.KeyPosition.NORMAL;
            keySlot.setRotate(key == Pfd.KeyPosition.NORMAL ? -KEY_ANGLE : KEY_ANGLE);
            keySwitchMode(key);
        });
        return keySwitch;
    }
}
