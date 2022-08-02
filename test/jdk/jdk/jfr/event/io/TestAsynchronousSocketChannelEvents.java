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

import static jdk.test.lib.Asserts.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * @run main/othervm -Xlog:jfr+system=trace jdk.jfr.event.io.TestAsynchronousSocketChannelEvents
 */
public class TestAsynchronousSocketChannelEvents {
    private static final int bufSizeA = 10;
    private static final int bufSizeB = 20;

    private List<IOEvent> expectedEvents = new ArrayList<>();

    private synchronized void addExpectedEvent(IOEvent event) {
        expectedEvents.add(event);
    }

    public static void main(String[] args) throws Throwable {
        new TestAsynchronousSocketChannelEvents().test();
    }

    public void test() throws Throwable {
        try (Recording recording = new Recording()) {
            try (AsynchronousServerSocketChannel ss = AsynchronousServerSocketChannel.open()) {
                recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
                recording.start();

                ss.bind(null);
                var serverAddress = ss.getLocalAddress();

                TestThread readerThread = new TestThread(new XRun() {
                    @Override
                    public void xrun() throws IOException, ExecutionException, InterruptedException {
                        ByteBuffer bufA = ByteBuffer.allocate(bufSizeA);
                        ByteBuffer bufB = ByteBuffer.allocate(bufSizeB);
                        try (AsynchronousSocketChannel sc = ss.accept().get()) {
                            int readSize = sc.read(bufA).get();
                            assertEquals(readSize, bufSizeA, "Wrong readSize bufA");

                            addExpectedEvent(IOEvent.createSocketReadEvent(bufSizeA, sc.getRemoteAddress()));
                            /*
                            bufA.clear();
                            bufA.limit(1);
                            readSize = (int) sc.read(new ByteBuffer[] { bufA, bufB }, 0, 2, 0, TimeUnit.MILLISECONDS, null, null).get();
                            assertEquals(readSize, 1 + bufSizeB, "Wrong readSize 1+bufB");
                            addExpectedEvent(IOEvent.createSocketReadEvent(readSize, sc.socket()));

                             */

                            // We try to read, but client have closed. Should
                            // get EOF.
                            bufA.clear();
                            bufA.limit(1);
                            readSize = sc.read(bufA).get();
                            assertEquals(readSize, -1, "Wrong readSize at EOF");
                            addExpectedEvent(IOEvent.createSocketReadEvent(-1, sc.getRemoteAddress()));
                        }
                    }
                });
                readerThread.start();

                try (AsynchronousSocketChannel sc = AsynchronousSocketChannel.open()) {
                    sc.connect(serverAddress).get();
                    ByteBuffer bufA = ByteBuffer.allocateDirect(bufSizeA);
                    ByteBuffer bufB = ByteBuffer.allocateDirect(bufSizeB);
                    for (int i = 0; i < bufSizeA; ++i) {
                        bufA.put((byte) ('a' + (i % 20)));
                    }
                    for (int i = 0; i < bufSizeB; ++i) {
                        bufB.put((byte) ('A' + (i % 20)));
                    }
                    bufA.flip();
                    bufB.flip();
                    Future<Integer> f = sc.write(bufA);
                    int nA = f.get();
                    assertEquals(nA, bufSizeA, "Wrong bytesWritten bufA");
                    addExpectedEvent(IOEvent.createSocketWriteEvent(bufSizeA, sc.getRemoteAddress()));

                    /*
                    bufA.clear();
                    bufA.limit(1);
                    int bytesWritten = (int) sc.write(new ByteBuffer[] { bufA, bufB });
                    assertEquals(bytesWritten, 1 + bufSizeB, "Wrong bytesWritten 1+bufB");
                    addExpectedEvent(IOEvent.createSocketWriteEvent(bytesWritten, sc.socket()));

                     */
                }

                readerThread.joinAndThrow();
                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                // AsynchronousSocketChannelImpl is failing to be instrumented currently
                IOHelper.verifyEquals(events, expectedEvents);
            }
        }
    }
}
