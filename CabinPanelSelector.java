import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Shows the cabin panel for one elevator at a time, with arrows on each side of the
 * elevator number to switch elevators.
 */
public class CabinPanelSelector extends StackPane {

    private static final double TITLE_ROW = 86;

    private final CabinPanel[] panels = new CabinPanel[Pfd.ELEVATORS + 1];
    private int shown;

    public CabinPanelSelector(SoftwareBus bus, int startElevator) {
        for (int elevator = 1; elevator <= Pfd.ELEVATORS; elevator++) {
            panels[elevator] = new CabinPanel(bus, elevator);
            getChildren().add(panels[elevator]);
        }

        PanelParts.RoundButton previous = new PanelParts.RoundButton(28, PanelParts.cabinArrow(false), () -> step(-1));
        PanelParts.RoundButton next = new PanelParts.RoundButton(28, PanelParts.cabinArrow(true), () -> step(1));
        HBox arrows = new HBox(260, previous, next);
        arrows.setAlignment(Pos.CENTER);
        arrows.setPrefHeight(TITLE_ROW);
        arrows.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        getChildren().add(arrows);

        setAlignment(Pos.TOP_CENTER);
        show(startElevator);
    }

    public void show(int elevator) {
        if (elevator >= 1 && elevator <= Pfd.ELEVATORS) {
            shown = elevator;
            for (int e = 1; e <= Pfd.ELEVATORS; e++) {
                panels[e].setVisible(e == elevator);
            }
        }
    }

    /** Moves to the next or previous elevator, wrapping around from the last back to 1. */
    private void step(int by) {
        show((shown - 1 + by + Pfd.ELEVATORS) % Pfd.ELEVATORS + 1);
    }
}
