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

package jdk.jfr.internal.instrument;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jdk.jfr.events.EventConfigurations;
import jdk.jfr.events.SocketReadEvent;
import jdk.jfr.events.SocketWriteEvent;
import jdk.jfr.internal.event.EventConfiguration;

/**
 * See {@link JITracer} for an explanation of this code.
 */
@JIInstrumentationTarget("sun.nio.ch.AsynchronousSocketChannelImpl")
final class AsynchronousSocketChannelImplInstrumentor {

    private AsynchronousSocketChannelImplInstrumentor() {
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    private <V extends Number,A> Future<V> read(boolean isScatteringRead,
                                                ByteBuffer dst,
                                                ByteBuffer[] dsts,
                                                long timeout,
                                                TimeUnit unit,
                                                A att,
                                                CompletionHandler<V,? super A> handler)
    {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_READ;
        if (!eventConfiguration.isEnabled()) {
            return read(isScatteringRead, dst, dsts, timeout, unit, att, handler);
        }

        long start = EventConfiguration.timestamp();
        return read(isScatteringRead, dst, dsts, timeout, unit, att, new CompletionHandler<V, A>() {
            @Override
            public void completed(V result, A attachment) {
                SocketAddress remote = null;
                try {
                    remote = getRemoteAddress();
                } catch (IOException ioe) { }
                SocketReadEvent.processEvent(start, result.intValue(), remote);

                // forward to the real handler
                if (handler != null) {
                    handler.completed(result, attachment);
                }
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                SocketAddress remote = null;
                try {
                    remote = getRemoteAddress();
                } catch (IOException ioe) { }
                SocketReadEvent.processEvent(start, 0, remote);

                // forward to the real handler
                if (handler != null) {
                    handler.failed(exc, attachment);
                }
            }
        });

    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    private <V extends Number,A> Future<V> write(boolean isGatheringWrite,
                                                 ByteBuffer src,
                                                 ByteBuffer[] srcs,
                                                 long timeout,
                                                 TimeUnit unit,
                                                 A att,
                                                 CompletionHandler<V,? super A> handler)
    {
        System.out.println("** instrumented write **\n");
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_WRITE;
        if (!eventConfiguration.isEnabled()) {
            return write(isGatheringWrite, src, srcs, timeout, unit, att, handler);
        }
        long start = EventConfiguration.timestamp();
        return write(isGatheringWrite, src, srcs, timeout, unit, att, new CompletionHandler<V, A>() {
            @Override
            public void completed(V result, A attachment) {
                SocketAddress remote = null;
                try {
                    remote = getRemoteAddress();
                } catch (IOException ioe) { }
                SocketWriteEvent.processEvent(start, result.intValue(), remote);

                // forward to the real handler
                if (handler != null) {
                    handler.completed(result, attachment);
                }
            }

            @Override
            public void failed(Throwable exc, A attachment) {
                SocketAddress remote = null;
                try {
                    remote = getRemoteAddress();
                } catch (IOException ioe) { }
                SocketWriteEvent.processEvent(start, 0, remote);

                // forward to the real handler
                if (handler != null) {
                    handler.failed(exc, attachment);
                }
            }
        });
    }

    public SocketAddress getRemoteAddress() throws IOException {
        // gets replaced by call to instrumented class
        return null;
    }

}
