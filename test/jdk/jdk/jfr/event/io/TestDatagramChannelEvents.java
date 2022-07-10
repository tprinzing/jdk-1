/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.jfr.event.io;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestDatagramChannelEvents
 */
public class TestDatagramChannelEvents {

    private String msg1 = "some message";
    private int msg1Length = msg1.getBytes().length;
    private List<IOEvent> expectedEvents = new ArrayList<>();

    private synchronized void addExpectedEvent(IOEvent event) {
        expectedEvents.add(event);
    }

    public static void main(String[] args) throws Throwable {
        new TestDatagramChannelEvents().testUnconnected();
        new TestDatagramChannelEvents().testConnected();
    }

    public void testUnconnected() throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
            recording.start();
            try (DatagramChannel server = DatagramChannel.open()) {
                server.bind(new InetSocketAddress("localhost", 0));

                try (DatagramChannel client = DatagramChannel.open()) {
                    client.bind(null);

                    ByteBuffer out = ByteBuffer.wrap(msg1.getBytes());
                    client.send(out, server.getLocalAddress());
                    addExpectedEvent(IOEvent.createDatagramWriteEvent(msg1Length, (InetSocketAddress) server.getLocalAddress()));

                    ByteBuffer in = ByteBuffer.allocate(out.capacity());
                    SocketAddress remoteAddr = server.receive(in);
                    String message = extractMessage(in);
                    addExpectedEvent(IOEvent.createDatagramReadEvent(msg1Length, (InetSocketAddress)remoteAddr));

                    recording.stop();
                    List<RecordedEvent> events = Events.fromRecording(recording);
                    IOHelper.verifyEquals(events, expectedEvents);
                }
            }
        }
    }
    public void testConnected() throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
            recording.start();
            try (DatagramChannel server = DatagramChannel.open()) {
                server.bind(new InetSocketAddress("localhost", 0));
                InetSocketAddress serverAddress = (InetSocketAddress) server.getLocalAddress();
                try (DatagramChannel client = DatagramChannel.open()) {
                    client.bind(null);
                    client.connect(serverAddress);
                    server.connect(client.getLocalAddress());

                    ByteBuffer out = ByteBuffer.wrap(msg1.getBytes());
                    client.write(out);
                    addExpectedEvent(IOEvent.createDatagramWriteEvent(msg1Length, serverAddress));

                    ByteBuffer in = ByteBuffer.allocate(out.capacity());
                    server.read(in);
                    String message = extractMessage(in);
                    assert (msg1.equals(message));
                    addExpectedEvent(IOEvent.createDatagramReadEvent(msg1Length, (InetSocketAddress) client.getLocalAddress()));

                    recording.stop();
                    List<RecordedEvent> events = Events.fromRecording(recording);
                    IOHelper.verifyEquals(events, expectedEvents);
                }
            }
        }
    }
    private static String extractMessage(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

}
