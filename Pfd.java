/**
 * Values and bus topics used by the passenger facing devices (PFD document, sections 2.3 and 3.3).
 *
 * Every command and event in the document's APIs has a topic here. The topic numbers are
 * placeholders until the full system assigns its own; changing them here is all it takes.
 */
public final class Pfd {

    public static final int FLOORS = 10;
    public static final int ELEVATORS = 4;

    // Sent in a message body as their ordinal
    public enum Direction { NONE, UP, DOWN }

    public enum KeyPosition { NORMAL, MANUAL }

    public enum Message { NONE, MANUAL, OVERLOAD, FIRE, OFF }

    // Cabin panel commands (controller to panel). Subtopic: elevator 1 to 4, or 0 for every cabin.
    public static final int CABIN_SET_FLOOR = 100;            // body: floor
    public static final int CABIN_SET_DIRECTION = 101;        // body: Direction
    public static final int CABIN_SET_MESSAGE = 102;          // body: Message
    public static final int CABIN_SET_BUTTON_LIGHT = 103;     // body: floor to turn on, or -floor to turn off
    public static final int CABIN_CLEAR_BUTTON_LIGHTS = 104;  // body: unused
    public static final int CABIN_SET_BUZZER = 105;           // body: 1 on, 0 off

    // Cabin panel events (panel to controller). Subtopic: elevator 1 to 4.
    public static final int CABIN_FLOOR_BUTTON_PRESSED = 110; // body: floor
    public static final int CABIN_KEY_SWITCH = 111;           // body: KeyPosition

    // Hall panel elevator rows (controller to panel). Every floor shows the same rows,
    // so these go to every hall panel. Subtopic: elevator 1 to 4.
    public static final int HALL_SET_ELEVATOR_FLOOR = 200;          // body: floor
    public static final int HALL_SET_ELEVATOR_DIRECTION = 201;      // body: Direction
    public static final int HALL_SET_ELEVATOR_OUT_OF_SERVICE = 202; // body: 1 on, 0 off

    // Hall panel commands (controller to panel). Subtopic: floor 1 to 10, or 0 for every floor.
    public static final int HALL_SET_UP_LIGHT = 210;    // body: 1 on, 0 off
    public static final int HALL_SET_DOWN_LIGHT = 211;  // body: 1 on, 0 off
    public static final int HALL_SET_FIRE_LIGHT = 212;  // body: 1 on, 0 off
    public static final int HALL_PLAY_CHIME = 213;      // body: unused

    // Hall panel events (panel to controller). Subtopic: floor 1 to 10.
    public static final int HALL_UP_BUTTON_PRESSED = 220;   // body: unused
    public static final int HALL_DOWN_BUTTON_PRESSED = 221; // body: unused
    public static final int HALL_FIRE_ALARM_PRESSED = 222;  // body: unused

    private Pfd() {
    }

    /** Turns a message body back into an enum value, or null if it is out of range. */
    public static <E extends Enum<E>> E decode(Class<E> type, int body) {
        E[] values = type.getEnumConstants();
        return body >= 0 && body < values.length ? values[body] : null;
    }

    public static boolean validFloor(int floor) {
        return floor >= 1 && floor <= FLOORS;
    }
}
