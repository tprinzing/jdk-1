package jdk.jfr.internal.spi;

import jdk.internal.event.EventGateway;
import jdk.jfr.events.DatagramSendEvent;
import jdk.jfr.events.EventConfigurations;
import jdk.jfr.events.SocketReadEvent;
import jdk.jfr.events.SocketWriteEvent;
import jdk.jfr.internal.event.EventConfiguration;

import java.net.SocketAddress;

/**
 * Provides a JFR event logging service to the java.base
 * module.
 */
public final class JFREventGateway implements EventGateway {

    @Override
    public NetworkEventPublisher datagramSend() {
        return datagramSendPublish;
    }

    @Override
    public NetworkEventPublisher datagramReceive() {
        return datagramReceivePublish;
    }

    @Override
    public NetworkEventPublisher socketRead() {
        return socketReadPublisher;
    }

    @Override
    public NetworkEventPublisher socketWrite() {
        return socketWritePublisher;
    }

    private final NetworkEventPublisher datagramSendPublish = new NetworkEventPublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.DATAGRAM_SEND;
            System.out.println(getClass().getName()+ " config: "+config);
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void log(long start, long byteCount, SocketAddress remote) {
            System.out.println(getClass().getName()+ " processEvent");
            DatagramSendEvent.processEvent(start, byteCount, remote);
        }
    };

   private final NetworkEventPublisher datagramReceivePublish = new NetworkEventPublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.DATAGRAM_RECEIVE;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void log(long start, long bytesRead, SocketAddress remote) {
            jdk.jfr.events.DatagramReceiveEvent.processEvent(start, bytesRead, remote);
        }

   };

    private final NetworkEventPublisher socketReadPublisher = new NetworkEventPublisher() {
        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.SOCKET_READ;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void log(long start, long bytesRead, SocketAddress remote) {
            SocketReadEvent.processEvent(start, bytesRead, remote);
        }
    };

    private final NetworkEventPublisher socketWritePublisher = new NetworkEventPublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.SOCKET_WRITE;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void log(long start, long byteCount, SocketAddress remote) {
            SocketWriteEvent.processEvent(start, byteCount, remote);
        }

    };
}
