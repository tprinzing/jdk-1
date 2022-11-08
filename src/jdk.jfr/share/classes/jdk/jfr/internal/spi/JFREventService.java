/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.internal.spi;

import jdk.internal.event.*;
import jdk.jfr.events.*;
import jdk.jfr.internal.event.EventConfiguration;

import java.net.SocketAddress;

/**
 * Provides a JFR event logging service to the java.base
 * module.
 */
public final class JFREventService implements EventService {

    @Override
    public DatagramSendPublisher datagramSend() {
        return datagramSendPublish;
    }

    @Override
    public DatagramReceivePublisher datagramReceive() {
        return datagramReceivePublish;
    }

    @Override
    public SocketReadPublisher socketRead() {
        return socketReadPublisher;
    }

    @Override
    public SocketWritePublisher socketWrite() {
        return socketWritePublisher;
    }

    private final DatagramSendPublisher datagramSendPublish = new DatagramSendPublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.DATAGRAM_SEND;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public boolean shouldCommit(long duration) {
            return EventConfigurations.DATAGRAM_SEND.shouldCommit(duration);
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void commit(long start, long duration, String host, String address, int port, long bytes) {
            DatagramSendEvent.commit(start, duration, host, address, port, bytes);
        }
    };

   private final DatagramReceivePublisher datagramReceivePublish = new DatagramReceivePublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.DATAGRAM_RECEIVE;
            return (config != null) ? config.isEnabled() : false;
        }

       @Override
       public boolean shouldCommit(long duration) {
           return EventConfigurations.DATAGRAM_RECEIVE.shouldCommit(duration);
       }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void commit(long start, long duration, String host, String address, int port, long timeout, long byteRead) {
            DatagramReceiveEvent.commit(start, duration, host, address, port, timeout, byteRead);
        }
   };

    private final SocketReadPublisher socketReadPublisher = new SocketReadPublisher() {
        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.SOCKET_READ;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public boolean shouldCommit(long duration) {
            return EventConfigurations.SOCKET_READ.shouldCommit(duration);
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void commit(long start, long duration, String host, String address, int port, long timeout, long byteRead, boolean endOfStream) {
            SocketReadEvent.commit(start, duration, host, address, port, timeout, byteRead, endOfStream);
        }
    };

    private final SocketWritePublisher socketWritePublisher = new SocketWritePublisher() {

        @Override
        public boolean isEnabled() {
            EventConfiguration config = EventConfigurations.SOCKET_WRITE;
            return (config != null) ? config.isEnabled() : false;
        }

        @Override
        public boolean shouldCommit(long duration) {
            return EventConfigurations.SOCKET_WRITE.shouldCommit(duration);
        }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void commit(long start, long duration, String host, String address, int port, long bytes) {
            SocketWriteEvent.commit(start, duration, host, address, port, bytes);
        }
    };
}
