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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import jdk.jfr.events.EventConfigurations;
import jdk.jfr.events.SocketReadEvent;
import jdk.jfr.events.SocketWriteEvent;
import jdk.jfr.internal.event.EventConfiguration;

/**
 * See {@link JITracer} for an explanation of this code.
 */
@JIInstrumentationTarget("sun.nio.ch.DatagramChannelImpl")
final class DatagramChannelImplInstrumentor {

    private DatagramChannelImplInstrumentor() {
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    SocketAddress blockingReceive(ByteBuffer dst, long nanos) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_READ;
        if (!eventConfiguration.isEnabled()) {
            return blockingReceive(dst, nanos);
        }
        int bytesRead = 0;
        long start  = 0;
        SocketAddress remoteAddress = null;
        try {
            long pos = dst.position();
            start = EventConfiguration.timestamp();;
            remoteAddress = blockingReceive(dst, nanos);
            bytesRead = (int) (dst.position() - pos);
        } finally {
            SocketReadEvent.processEvent(start, bytesRead, remoteAddress);
        }
        return remoteAddress;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public SocketAddress receive(ByteBuffer dst) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_READ;
        if (!eventConfiguration.isEnabled()) {
            return receive(dst);
        }
        int bytesRead = 0;
        long start  = 0;
        SocketAddress remoteAddress = null;
        try {
            long pos = dst.position();
            start = EventConfiguration.timestamp();;
            remoteAddress = receive(dst);
            bytesRead = (int) (dst.position() - pos);
        } finally {
            SocketReadEvent.processEvent(start, bytesRead, remoteAddress);
        }
        return remoteAddress;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int read(ByteBuffer dst) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_READ;
        if (!eventConfiguration.isEnabled()) {
            return read(dst);
        }
        int bytesRead = 0;
        long start  = 0;
        try {
            start = EventConfiguration.timestamp();;
            bytesRead = read(dst);
        } finally {
            SocketReadEvent.processEvent(start, bytesRead, getRemoteAddress());
        }
        return bytesRead;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_READ;
        if (!eventConfiguration.isEnabled()) {
            return read(dsts, offset, length);
        }
        long bytesRead = 0;
        long start = 0;
        try {
            start = EventConfiguration.timestamp();
            bytesRead = read(dsts, offset, length);
        } finally {
            SocketReadEvent.processEvent(start, bytesRead, getRemoteAddress());
        }
        return bytesRead;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int send(ByteBuffer src, SocketAddress target) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_WRITE;
        if (!eventConfiguration.isEnabled()) {
            return send(src, target);
        }
        int bytesWritten = 0;
        long start = 0;
        try {
            start = EventConfiguration.timestamp();
            bytesWritten = send(src, target);
        } finally {
            SocketWriteEvent.processEvent(start, bytesWritten, target);
        }
        return bytesWritten;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int write(ByteBuffer buf) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_WRITE;
        if (!eventConfiguration.isEnabled()) {
            return write(buf);
        }
        int bytesWritten = 0;
        long start = 0;
        try {
            start = EventConfiguration.timestamp();
            bytesWritten = write(buf);
        } finally {
            SocketWriteEvent.processEvent(start, bytesWritten, getRemoteAddress());
        }
        return bytesWritten;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        // gets replaced by call to instrumented class
        return null;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        EventConfiguration eventConfiguration = EventConfigurations.SOCKET_WRITE;
        if (!eventConfiguration.isEnabled()) {
            return write(srcs, offset, length);
        }
        long bytesWritten = 0;
        long start = 0;
        try {
            start = EventConfiguration.timestamp();
            bytesWritten = write(srcs, offset, length);
        } finally {
            SocketWriteEvent.processEvent(start, bytesWritten, getRemoteAddress());
        }
        return bytesWritten;
    }
}
