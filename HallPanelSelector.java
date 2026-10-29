import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Shows the hall panel for one floor at a time, with arrows on each side of the
 * floor number to switch floors.
 */
public class HallPanelSelector extends StackPane {

    private final HallPanel[] panels = new HallPanel[Pfd.FLOORS + 1];
    private int shown;

    public HallPanelSelector(SoftwareBus bus, int startFloor) {
        for (int floor = 1; floor <= Pfd.FLOORS; floor++) {
            panels[floor] = new HallPanel(bus, floor);
            getChildren().add(panels[floor]);
        }

        PanelParts.RoundButton previous = new PanelParts.RoundButton(28, PanelParts.hallArrow(false), () -> step(-1));
        PanelParts.RoundButton next = new PanelParts.RoundButton(28, PanelParts.hallArrow(true), () -> step(1));
        HBox arrows = new HBox(442, previous, next);
        arrows.setAlignment(Pos.CENTER);
        arrows.setPrefHeight(HallPanel.HEADER_HEIGHT);
        arrows.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        getChildren().add(arrows);

        setAlignment(Pos.TOP_CENTER);
        show(startFloor);
    }

    public void show(int floor) {
        if (Pfd.validFloor(floor)) {
            shown = floor;
            for (int f = 1; f <= Pfd.FLOORS; f++) {
                panels[f].setVisible(f == floor);
            }
        }
    }

    /** Moves up or down a floor, wrapping around from 10 back to 1. */
    private void step(int by) {
        show((shown - 1 + by + Pfd.FLOORS) % Pfd.FLOORS + 1);
    }
}
