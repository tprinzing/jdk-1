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
        public void log(long start, long byteCount, SocketAddress remote) {
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
       public boolean shouldCommit(long duration) {
           return EventConfigurations.DATAGRAM_RECEIVE.shouldCommit(duration);
       }

        @Override
        public long timestamp() {
            return EventConfiguration.timestamp();
        }

        @Override
        public void log(long start, long byteCount, SocketAddress remote) {
            jdk.jfr.events.DatagramReceiveEvent.processEvent(start, byteCount, remote);
        }

   };

    private final NetworkEventPublisher socketReadPublisher = new NetworkEventPublisher() {
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
        public void log(long start, long byteCount, SocketAddress remote) {
            SocketReadEvent.processEvent(start, byteCount, remote);
        }
    };

    private final NetworkEventPublisher socketWritePublisher = new NetworkEventPublisher() {

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
        public void log(long start, long byteCount, SocketAddress remote) {
            SocketWriteEvent.processEvent(start, byteCount, remote);
        }

    };
}
