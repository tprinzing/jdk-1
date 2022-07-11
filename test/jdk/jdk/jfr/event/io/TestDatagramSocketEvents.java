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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.thread.TestThread;
import jdk.test.lib.thread.XRun;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestDatagramSocketEvents
 */
public class TestDatagramSocketEvents {

    private static String msg1 = "some message";
    private static int msg1Length = msg1.getBytes().length;
    private List<IOEvent> expectedEvents = new ArrayList<>();

    private synchronized void addExpectedEvent(IOEvent event) {
        expectedEvents.add(event);
    }

    public static void main(String[] args) throws Throwable {
        new TestDatagramSocketEvents().testUnconnected();
    }

    public void testUnconnected() throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
            recording.start();
            try (DatagramSocket server = new DatagramSocket(new InetSocketAddress("localhost", 0))) {
                InetSocketAddress saddr = new InetSocketAddress(server.getLocalAddress(), server.getLocalPort());

                TestThread readerThread = new TestThread(new XRun() {
                    @Override
                    public void xrun() throws IOException {
                        var packetIn = new DatagramPacket(new byte[msg1Length], msg1Length);
                        server.receive(packetIn);
                        String message = new String(packetIn.getData(), 0, packetIn.getLength(), Charset.defaultCharset());
                        assert(msg1.equals(message));
                        addExpectedEvent(IOEvent.createDatagramReadEvent(msg1Length, (InetSocketAddress)packetIn.getSocketAddress()));
                    }
                });
                readerThread.start();

                try (DatagramSocket client = new DatagramSocket()) {
                    ByteBuffer out = ByteBuffer.wrap(msg1.getBytes());
                    var packetOut = new DatagramPacket(msg1.getBytes(), msg1Length, saddr );
                    client.send(packetOut);
                    addExpectedEvent(IOEvent.createDatagramWriteEvent(msg1Length, saddr));
                }

                readerThread.joinAndThrow();
                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                IOHelper.verifyEquals(events, expectedEvents);

            }
        }
    }

}
