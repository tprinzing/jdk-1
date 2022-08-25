package jdk.internal.event;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public interface EventGateway {

    private static EventGateway locateService() {
        EventGateway svc = null;
        try {
            var s = ServiceLoader.load(EventGateway.class).findFirst();
            if (s.isPresent()) {
                svc = s.get();
            }
        } catch (ServiceConfigurationError e) {
            e.printStackTrace();
        } finally {
            if (svc == null) {
                svc = new EventGateway() { };
            }
        }
        return svc;
    }

    /**
     * The service to use for all event logging.
     */
    static final EventGateway service = locateService();

    /**
     *
     * @return the object to use to publish network events for socket write
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher socketRead() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for socket read
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher socketWrite() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram send
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher datagramSend() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram receive
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher datagramReceive() { return noPublish; }

    static final NetworkEventPublisher noPublish = new NetworkEventPublisher() { };
    interface NetworkEventPublisher {

        default boolean isEnabled() { return false; }

        default long timestamp() { return 0; }

        default void log(long start, long bytesRead, SocketAddress remote) {}
    }
}
