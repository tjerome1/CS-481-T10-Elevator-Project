import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.util.Duration;

/** Pieces shared by the cabin panel and the hall panel. */
final class PanelParts {

    static final String FONT = "Arial";
    static final Color TEXT = Color.web("#222222");
    static final Color MUTED = Color.web("#666666");
    static final Color BORDER = Color.web("#888888");
    static final Color BLUE = Color.web("#2b7bd6");
    static final Color LIT_FILL = Color.web("#e6f0fa");
    static final Color RED = Color.web("#e04444");
    static final Color GRAY = Color.web("#d0d0d0");

    private PanelParts() {
    }

    static Label label(String text, double size, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font(FONT, size));
        label.setTextFill(color);
        // Let captions run past the edge of their grid column instead of being cut off
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    static String box(int radius) {
        return "-fx-background-color: white; -fx-border-color: #888888; -fx-border-width: 2;"
                + " -fx-border-radius: " + radius + "; -fx-background-radius: " + radius + ";";
    }

    /** Grid with fixed column widths, so the same columns line up from one row to the next. */
    static GridPane columns(double... widths) {
        GridPane grid = new GridPane();
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints(width, width, width);
            column.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(column);
        }
        grid.setMaxWidth(Region.USE_PREF_SIZE);
        return grid;
    }

    static RowConstraints centeredRow() {
        RowConstraints row = new RowConstraints();
        row.setValignment(VPos.CENTER);
        return row;
    }

    /** Direction arrow that starts gray and turns blue with setLit. */
    static Polygon arrow(boolean up, double size) {
        double h = size * 0.8;
        Polygon arrow = up
                ? new Polygon(size / 2, 0, size, h, 0, h)
                : new Polygon(0, 0, size, 0, size / 2, h);
        arrow.setFill(GRAY);
        return arrow;
    }

    static void setLit(Polygon arrow, boolean lit) {
        arrow.setFill(lit ? BLUE : GRAY);
    }

    /** Left or right arrow for the buttons that switch floors on the hall panel. */
    static Polyline hallArrow(boolean right) {
        return switchArrow(right);
    }

    /** Left or right arrow for the buttons that switch elevators on the cabin panel. */
    static Polyline cabinArrow(boolean right) {
        return switchArrow(right);
    }

    private static Polyline switchArrow(boolean right) {
        Polyline arrow = right
                ? new Polyline(0, 0, 9, 10, 0, 20)
                : new Polyline(9, 0, 0, 10, 9, 20);
        arrow.setStroke(TEXT);
        arrow.setStrokeWidth(2.5);
        return arrow;
    }

    /** Polls the bus for the given topics on the JavaFX thread and hands each message to the handler. */
    static void listen(SoftwareBus bus, int id, int subtopic, Consumer<SoftwareBus.BusMessage> handler, int... topics) {
        for (int topic : topics) {
            bus.subscribe(id, topic, subtopic);
        }
        Timeline poll = new Timeline(new KeyFrame(Duration.millis(30), e -> {
            for (int topic : topics) {
                SoftwareBus.BusMessage message;
                while ((message = bus.getMessage(id, topic, subtopic)) != null) {
                    handler.accept(message);
                }
            }
        }));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
    }

    /** Round push button that lights up (blue unless changed). Pressing it only runs the action; the light is set separately. */
    static class RoundButton extends StackPane {

        private final Circle circle;
        private boolean lit;
        private Color litFill = LIT_FILL;
        private Color litStroke = BLUE;

        RoundButton(double radius, Node face, Runnable onPress) {
            circle = new Circle(radius);
            getChildren().addAll(circle, face);
            paint();
            setCursor(Cursor.HAND);
            setOnMousePressed(e -> {
                circle.setFill(Color.web("#d8d8d8"));
                onPress.run();
            });
            setOnMouseReleased(e -> paint());
        }

        void setLit(boolean on) {
            lit = on;
            paint();
        }

        void setLitColors(Color fill, Color stroke) {
            litFill = fill;
            litStroke = stroke;
            paint();
        }

        private void paint() {
            circle.setFill(lit ? litFill : Color.WHITE);
            circle.setStroke(lit ? litStroke : BORDER);
            circle.setStrokeWidth(lit ? 4 : 2);
        }
    }

    /** Round speaker grille made of rings of small holes. The rim changes color while it is sounding. */
    static class Speaker extends Group {

        private final Circle rim;

        Speaker(double radius) {
            rim = new Circle(radius);
            rim.setFill(Color.web("#f4f4f4"));
            getChildren().add(rim);
            setSounding(null);

            double spacing = 8.5;
            int rings = (int) ((radius - 9) / spacing);
            for (int ring = 0; ring <= rings; ring++) {
                double r = ring * spacing;
                int count = Math.max(1, ring * 6);
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    Circle hole = new Circle(r * Math.cos(angle), r * Math.sin(angle), 2.6);
                    hole.setFill(Color.web("#444444"));
                    getChildren().add(hole);
                }
            }
        }

        /** color is the rim color while sounding, or null when quiet. */
        void setSounding(Color color) {
            rim.setStroke(color == null ? BORDER : color);
            rim.setStrokeWidth(color == null ? 2 : 4);
        }
    }
}
