import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.media.AudioClip;

/**
 * Sounds for the panel speakers, played from .mp3 files.
 *
 * If a file is missing or cannot be played, nothing is played.
 * Paths are relative to the folder the program is run from.
 */
final class Sound {

    // Placeholders: put the real sound files at these paths
    static final String BUZZER_FILE = "sounds/buzzer.mp3";
    static final String CHIME_FILE = "sounds/chime.mp3";
    static final String FIRE_ALARM_FILE = "sounds/firealarm.mp3";

    private static final Set<String> warned = new HashSet<>();
    private static AudioClip chime;

    /** One fire alarm for the whole building, so ten hall panels turning on play it only once. */
    static final Loop FIRE_ALARM = new Loop(FIRE_ALARM_FILE);

    private Sound() {
    }

    /** Arrival chime for the hall panel. */
    static synchronized void chime() {
        if (chime == null) {
            chime = load(CHIME_FILE);
        }
        play(chime);
    }

    /**
     * Sound that repeats while it is on, like a cabin's buzzer or the fire alarm.
     * Several panels can turn it on; it keeps playing until every one of them turns it off.
     */
    static final class Loop {

        private final String path;
        private final Set<Object> sources = new HashSet<>();
        private AudioClip clip;

        Loop(String path) {
            this.path = path;
        }

        synchronized void set(Object source, boolean on) {
            if (on) {
                sources.add(source);
            } else {
                sources.remove(source);
            }
            if (clip == null) {
                clip = load(path);
                if (clip == null) {
                    return;
                }
                clip.setCycleCount(AudioClip.INDEFINITE);
            }
            if (sources.isEmpty()) {
                clip.stop();
            } else if (!clip.isPlaying()) {
                play(clip);
            }
        }
    }

    /** Returns the clip for the file, or null if it is missing or cannot be loaded. */
    private static synchronized AudioClip load(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            warnOnce(path, "Sound file " + path + " not found, playing nothing.");
            return null;
        }
        try {
            return new AudioClip(file.toURI().toString());
        } catch (RuntimeException e) {
            warnOnce(path, "Could not load " + path + ", playing nothing: " + e.getMessage());
            return null;
        }
    }

    private static void play(AudioClip clip) {
        if (clip == null) {
            return;
        }
        try {
            clip.play();
        } catch (RuntimeException e) {
            warnOnce(clip.getSource(), "Could not play " + clip.getSource() + ": " + e.getMessage());
        }
    }

    private static synchronized void warnOnce(String key, String message) {
        if (warned.add(key)) {
            System.err.println(message);
        }
    }
}
