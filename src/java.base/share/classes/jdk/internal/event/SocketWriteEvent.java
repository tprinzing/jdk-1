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
package jdk.internal.event;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class SocketWriteEvent {

    /**
     * Are events of this type enabled?
     * @return
     */
    public static boolean isEnabled() {
        if (handler == null) {
            try {
                Optional<Provider> p = ServiceLoader.load(Provider.class).findFirst();
                if (p.isPresent()) {
                    handler = p.get();
                }
            } catch (ServiceConfigurationError e) {

            } finally {
                if (handler == null) {
                    handler = new Provider() {
                        @Override
                        public boolean isEnabled() {
                            return false;
                        }

                        @Override
                        public long timestamp() {
                            return 0;
                        }

                        @Override
                        public void processEvent(long start, long bytesRead, SocketAddress remote) {

                        }
                    };
                }
            }
        }
        return handler.isEnabled();
    }

    /**
     * Fetch a timestamp to indicate the start of a read.
     * @return
     */
    public static long timestamp() {
        return handler.timestamp();
    }

    /**
     * Potentially log an event.  This should be called if after the read is complete if
     * {@link #isEnabled()} returns true.
     *
     * @param start  the start time of read operation (value from a previous call to {@link #timestamp()}.
     * @param bytesRead  how many bytes were received
     * @param remote  the address of the remote socket being read from
     */
    public static void processEvent(long start, long bytesRead, SocketAddress remote) {
        handler.processEvent(start, bytesRead, remote);
    }

    private static Provider handler;

    public interface Provider {

        boolean isEnabled();

        long timestamp();

        void processEvent(long start, long bytesRead, SocketAddress remote);
    }
}
