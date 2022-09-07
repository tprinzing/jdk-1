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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface EventGateway {

    private static EventGateway locateService() {
        EventGateway svc = null;
        try {
            var s = ServiceLoader.load(EventGateway.class).findFirst();
            if (s.isPresent()) {
                svc = s.get();
            }
        } catch (ServiceConfigurationError e) {
            e.printStackTrace();
        } finally {
            if (svc == null) {
                svc = new EventGateway() { };
            }
        }
        return svc;
    }

    /**
     * The service to use for all event logging.
     */
    static final EventGateway service = locateService();

    /**
     *
     * @return the object to use to publish network events for socket write
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher socketRead() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for socket read
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher socketWrite() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram send
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher datagramSend() { return noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram receive
     *   operations.  The default object will not attempt to log any events.
     */
    default NetworkEventPublisher datagramReceive() { return noPublish; }

    interface MeasuredFunction {

        long apply() throws IOException;
    }

    static final NetworkEventPublisher noPublish = new NetworkEventPublisher() { };
    interface NetworkEventPublisher {

        default long measure(SocketAddress address, MeasuredFunction function) throws IOException {
            if (! isEnabled()) {
                return function.apply();
            }
            long nbytes = 0;
            long start = 0;
            try {
                start =  timestamp();
                nbytes = function.apply();
            } finally {
                log(start, nbytes, address);
            }
            return nbytes;
        }

        default boolean isEnabled() { return false; }

        default boolean shouldCommit(long duration) { return false; }

        default long timestamp() { return 0; }

        default void log(long start, long bytesRead, SocketAddress remote) {}
    }
}
