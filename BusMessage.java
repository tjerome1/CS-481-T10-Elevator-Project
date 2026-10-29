/**
 * The message on the software bus. Has a topic, a subtopic, and an integer body.
 * Topic and subtopic are 2 bytes each and the body is 4, so a message is 8 bytes.
 */
public record BusMessage(int topic, int subtopic, int body) {

    public BusMessage {
        if (topic < 0 || topic > Short.MAX_VALUE || subtopic < 0 || subtopic > Short.MAX_VALUE) {
            throw new IllegalArgumentException("topic and subtopic must be from 0 to " + Short.MAX_VALUE);
        }
    }
}
