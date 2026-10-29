import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Mock manager for testing the panels without the rest of the system.
 *
 * It is always listening on the bus and prints every button press and key turn it receives.
 * Commands typed into the console are sent over the bus to the displays (type "help" for the list).
 * Take this class out and the panels work the same with the real controllers.
 */
public class TestHarness {

    private static final int[] EVENTS = {
            Pfd.CABIN_FLOOR_BUTTON_PRESSED, Pfd.CABIN_KEY_SWITCH,
            Pfd.HALL_UP_BUTTON_PRESSED, Pfd.HALL_DOWN_BUTTON_PRESSED, Pfd.HALL_FIRE_ALARM_PRESSED };

    /** Opens the panel windows. */
    public interface Windows {
        void openHall();

        void openCabin(int elevator);

        void openAll();

        void quit();
    }

    private static final String HELP = """
            Windows:
              open hall                               (hall panel, the arrows change floors)
              open cabin <1-4>                        (cabin panel for one elevator)
              open all                                (hall panel and all four cabin panels)
              quit
            Test harness commands (elevator 0 or floor 0 sends to all of them):
              cabin <elevator> floor <1-10>
              cabin <elevator> direction up|down|none
              cabin <elevator> message none|manual|overload|fire|off
              cabin <elevator> light <floor> on|off
              cabin <elevator> clearlights
              cabin <elevator> buzzer on|off
              hall <floor> uplight on|off
              hall <floor> downlight on|off
              hall <floor> fire on|off
              hall <floor> chime
              row <elevator> floor <1-10>             (elevator rows on every hall panel)
              row <elevator> direction up|down|none
              row <elevator> outofservice on|off
              send <topic> <subtopic> <body>          (any raw bus message)
              help""";

    private final SoftwareBus bus;
    private final Windows windows;
    private final int busId;

    public TestHarness(SoftwareBus bus, Windows windows) {
        this.bus = bus;
        this.windows = windows;
        this.busId = bus.register();
        for (int topic : EVENTS) {
            bus.subscribe(busId, topic, SoftwareBus.ALL);
        }
    }

    /** Starts listening on the bus and reading commands from the console. */
    public void start() {
        System.out.println(HELP);
        background("harness-listener", this::listen);
        background("harness-console", this::readConsole);
    }

    private void listen() {
        while (true) {
            for (int topic : EVENTS) {
                BusMessage message;
                while ((message = bus.getMessage(busId, topic, SoftwareBus.ALL)) != null) {
                    System.out.println("Test harness received " + describe(message) + "  " + raw(message));
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static String describe(BusMessage m) {
        return switch (m.topic()) {
            case Pfd.CABIN_FLOOR_BUTTON_PRESSED ->
                    "from elevator " + m.subtopic() + " cabin panel: floorButtonPressed(" + m.body() + ")";
            case Pfd.CABIN_KEY_SWITCH ->
                    "from elevator " + m.subtopic() + " cabin panel: keySwitchMode("
                            + Pfd.decode(Pfd.KeyPosition.class, m.body()) + ")";
            case Pfd.HALL_UP_BUTTON_PRESSED -> "from floor " + m.subtopic() + " hall panel: upButtonPressed()";
            case Pfd.HALL_DOWN_BUTTON_PRESSED -> "from floor " + m.subtopic() + " hall panel: downButtonPressed()";
            case Pfd.HALL_FIRE_ALARM_PRESSED -> "from floor " + m.subtopic() + " hall panel: fireAlarmPressed()";
            default -> "a message";
        };
    }

    private static String raw(BusMessage m) {
        return "[topic " + m.topic() + ", subtopic " + m.subtopic() + ", body " + m.body() + "]";
    }

    private void readConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] words = line.trim().toLowerCase(Locale.ROOT).split("\\s+");
                if (words[0].isEmpty()) {
                    continue;
                }
                try {
                    switch (words[0]) {
                        case "help" -> System.out.println(HELP);
                        case "open" -> open(words);
                        case "quit" -> {
                            windows.quit();
                            return;
                        }
                        default -> {
                            BusMessage message = parse(words);
                            bus.publish(busId, message);
                            System.out.println("Test harness sent " + raw(message));
                        }
                    }
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                    System.out.println("Could not understand \"" + line.trim() + "\". Type help for the commands.");
                }
            }
        } catch (IOException e) {
            System.err.println("Test harness stopped reading the console: " + e.getMessage());
        }
    }

    private void open(String[] w) {
        switch (w[1]) {
            case "hall" -> windows.openHall();
            case "cabin" -> windows.openCabin(number(w[2], 1, Pfd.ELEVATORS));
            case "all" -> windows.openAll();
            default -> throw new IllegalArgumentException(w[1]);
        }
    }

    private static BusMessage parse(String[] w) {
        switch (w[0]) {
            case "send":
                return new BusMessage(Integer.parseInt(w[1]), Integer.parseInt(w[2]), Integer.parseInt(w[3]));
            case "cabin": {
                int elevator = number(w[1], 0, Pfd.ELEVATORS);
                return switch (w[2]) {
                    case "floor" -> new BusMessage(Pfd.CABIN_SET_FLOOR, elevator, number(w[3], 1, Pfd.FLOORS));
                    case "direction" -> new BusMessage(Pfd.CABIN_SET_DIRECTION, elevator, choice(Pfd.Direction.class, w[3]));
                    case "message" -> new BusMessage(Pfd.CABIN_SET_MESSAGE, elevator, choice(Pfd.Message.class, w[3]));
                    case "light" -> {
                        int floor = number(w[3], 1, Pfd.FLOORS);
                        yield new BusMessage(Pfd.CABIN_SET_BUTTON_LIGHT, elevator, onOff(w[4]) == 1 ? floor : -floor);
                    }
                    case "clearlights" -> new BusMessage(Pfd.CABIN_CLEAR_BUTTON_LIGHTS, elevator, 0);
                    case "buzzer" -> new BusMessage(Pfd.CABIN_SET_BUZZER, elevator, onOff(w[3]));
                    default -> throw new IllegalArgumentException(w[2]);
                };
            }
            case "hall": {
                int floor = number(w[1], 0, Pfd.FLOORS);
                return switch (w[2]) {
                    case "uplight" -> new BusMessage(Pfd.HALL_SET_UP_LIGHT, floor, onOff(w[3]));
                    case "downlight" -> new BusMessage(Pfd.HALL_SET_DOWN_LIGHT, floor, onOff(w[3]));
                    case "fire" -> new BusMessage(Pfd.HALL_SET_FIRE_LIGHT, floor, onOff(w[3]));
                    case "chime" -> new BusMessage(Pfd.HALL_PLAY_CHIME, floor, 0);
                    default -> throw new IllegalArgumentException(w[2]);
                };
            }
            case "row": {
                int elevator = number(w[1], 1, Pfd.ELEVATORS);
                return switch (w[2]) {
                    case "floor" -> new BusMessage(Pfd.HALL_SET_ELEVATOR_FLOOR, elevator, number(w[3], 1, Pfd.FLOORS));
                    case "direction" -> new BusMessage(Pfd.HALL_SET_ELEVATOR_DIRECTION, elevator,
                            choice(Pfd.Direction.class, w[3]));
                    case "outofservice" -> new BusMessage(Pfd.HALL_SET_ELEVATOR_OUT_OF_SERVICE, elevator, onOff(w[3]));
                    default -> throw new IllegalArgumentException(w[2]);
                };
            }
            default:
                throw new IllegalArgumentException(w[0]);
        }
    }

    private static int number(String word, int min, int max) {
        int value = Integer.parseInt(word);
        if (value < min || value > max) {
            throw new IllegalArgumentException(word);
        }
        return value;
    }

    private static <E extends Enum<E>> int choice(Class<E> type, String word) {
        return Enum.valueOf(type, word.toUpperCase(Locale.ROOT)).ordinal();
    }

    private static int onOff(String word) {
        return switch (word) {
            case "on" -> 1;
            case "off" -> 0;
            default -> throw new IllegalArgumentException(word);
        };
    }

    private static void background(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
