/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.DataAmount;
import jdk.jfr.Name;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.event.EventConfiguration;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;

@Name(Type.EVENT_NAME_PREFIX + "SocketWrite")
@Label("Socket Write")
@Category("Java Application")
@Description("Writing data to a socket")
public final class SocketWriteEvent extends AbstractJDKEvent {

    // The order of these fields must be the same as the parameters in
    // commit(..., String, String, int, long)

    @Label("Remote Host")
    public String host;

    @Label("Remote Address")
    public String address;

    @Label("Remote Port")
    public int port;

    @Label("Bytes Written")
    @Description("Number of bytes written to the socket")
    @DataAmount
    public long bytesWritten;

    public static void commit(long start, long duration, String host, String address, int port, long bytes) {
        // Generated
    }

    /**
     * Execute the standard boilerplate that proceeds a potential call to the machine generated
     * commit method.
     *
     * @param start  the start time
     * @param bytesWritten  how many bytes were sent
     * @param remote  the address of the remote socket being written to
     */
    public static void processEvent(long start, long bytesWritten, SocketAddress remote) {
        long duration = EventConfiguration.timestamp() - start;
        if (EventConfigurations.SOCKET_WRITE.shouldCommit(duration)) {
            long bytes = bytesWritten < 0 ? 0 : bytesWritten;
            if (remote instanceof InetSocketAddress isa) {
                String hostString  = isa.getAddress().toString();
                int delimiterIndex = hostString.lastIndexOf('/');

                String host = hostString.substring(0, delimiterIndex);
                String address = hostString.substring(delimiterIndex + 1);
                int port = isa.getPort();
                SocketWriteEvent.commit(start, duration, host, address, port, bytes);
            } else if (remote instanceof UnixDomainSocketAddress) {
                UnixDomainSocketAddress udsa = (UnixDomainSocketAddress) remote;
                String path = "[" + udsa.getPath().toString() + "]";
                SocketWriteEvent.commit(start, duration, "Unix domain socket", path, 0, bytes);
            }
        }
    }
}
