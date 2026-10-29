import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Starts the software bus and the test harness. The hall panel and each cabin panel are
 * opened in their own windows from the console, or all four cabin panels can share one
 * window (type "help" for the commands).
 *
 */
public class Main extends Application implements TestHarness.Windows {

    private final SoftwareBus bus = new SoftwareBus();
    private final Map<String, Stage> windows = new HashMap<>();

    @Override
    public void start(Stage unused) {
        // Windows are opened from the console, so keep running while none are open
        Platform.setImplicitExit(false);
        new TestHarness(bus, this).start();
    }

    @Override
    public void openHall() {
        Platform.runLater(() -> show("hall", 0, "Hall Panel", () -> new HallPanelSelector(bus, 1)));
    }

    @Override
    public void openCabin(int elevator) {
        Platform.runLater(() -> show("cabin " + elevator, elevator, "Cabin Panel - Elevator " + elevator,
                () -> new CabinPanel(bus, elevator)));
    }

    @Override
    public void openCabins() {
        Platform.runLater(() -> show("cabins", 1, "Cabin Panels", () -> new CabinPanelSelector(bus, 1)));
    }

    @Override
    public void openAll() {
        openHall();
        for (int elevator = 1; elevator <= Pfd.ELEVATORS; elevator++) {
            openCabin(elevator);
        }
    }

    @Override
    public void quit() {
        Platform.exit();
    }

    /**
     * Shows a panel's window, making it the first time. Closing a window only hides it,
     * so the panel stays on the bus and looks the same when it is opened again.
     */
    private void show(String key, int slot, String title, Supplier<Region> panel) {
        Stage stage = windows.computeIfAbsent(key, k -> window(title, panel.get(), slot));
        stage.show();
        stage.toFront();
    }

    private static Stage window(String title, Region panel, int slot) {
        StackPane padded = new StackPane(panel);
        padded.setPadding(new Insets(30));
        padded.setStyle("-fx-background-color: white;");

        // Scale the whole panel to fit the window so it can be resized freely
        Group content = new Group(padded);
        StackPane holder = new StackPane(content);
        holder.setStyle("-fx-background-color: white;");
        holder.setMinSize(0, 0);
        Scene scene = new Scene(holder, Color.WHITE);
        holder.applyCss();
        holder.layout();
        double width = padded.getWidth();
        double height = padded.getHeight();

        // Start at a size that fits on the screen, side by side with the other windows
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double start = Math.min(1, screen.getHeight() * 0.85 / height);
        holder.setPrefSize(width * start, height * start);

        holder.widthProperty().addListener((obs, old, w) -> fit(content, holder, width, height));
        holder.heightProperty().addListener((obs, old, h) -> fit(content, holder, width, height));

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setX(screen.getMinX() + slot * (screen.getWidth() - width * start) / Pfd.ELEVATORS);
        stage.setY(screen.getMinY() + 20);
        return stage;
    }

    private static void fit(Group content, StackPane holder, double width, double height) {
        double scale = Math.min(holder.getWidth() / width, holder.getHeight() / height);
        content.setScaleX(scale);
        content.setScaleY(scale);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
