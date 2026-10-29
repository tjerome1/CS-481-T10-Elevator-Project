import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Software bus that passes messages between the subsystems.
 *
 * Each subsystem registers to get an id, subscribes to the topics it wants, and then
 * publishes messages or picks up the ones waiting for it. There is one queue for every
 * id, topic, and subtopic it subscribed to. The bus does not know about any particular
 * topic; it just delivers them.
 *
 * Subtopic 0 means "all": a message published with subtopic 0 goes to every subscriber
 * of that topic, and subscribing with subtopic 0 receives every subtopic of that topic.
 *
 * Safe to use from more than one thread.
 */
public class SoftwareBus {

    /** Subtopic for a message to everyone, or a subscription to every subtopic. */
    public static final int ALL = 0;

    private record Subscription(int id, int topic, int subtopic) {
    }

    private final Set<Integer> registered = new HashSet<>();
    private final Map<Subscription, Queue<BusMessage>> queues = new LinkedHashMap<>();
    private int nextId = 1;

    /** Registers with the bus and returns the id to use for every other call. */
    public synchronized int register() {
        registered.add(nextId);
        return nextId++;
    }

    public synchronized void subscribe(int id, int topic, int subtopic) {
        checkRegistered(id);
        queues.putIfAbsent(new Subscription(id, topic, subtopic), new ArrayDeque<>());
    }

    public synchronized void publish(int id, BusMessage message) {
        checkRegistered(id);
        for (Map.Entry<Subscription, Queue<BusMessage>> entry : queues.entrySet()) {
            Subscription s = entry.getKey();
            if (s.topic() == message.topic()
                    && (s.subtopic() == ALL || message.subtopic() == ALL || s.subtopic() == message.subtopic())) {
                entry.getValue().add(message);
            }
        }
    }

    /** Returns the next message waiting for this subscription, or null if there is none. */
    public synchronized BusMessage getMessage(int id, int topic, int subtopic) {
        Queue<BusMessage> queue = queues.get(new Subscription(id, topic, subtopic));
        return queue == null ? null : queue.poll();
    }

    private void checkRegistered(int id) {
        if (!registered.contains(id)) {
            throw new IllegalArgumentException("id " + id + " is not registered with the bus");
        }
    }
}
