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

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Service to publish events from the {@code java.base} module.  This is not
 * a general purpose service as it is only exported to the {@code jdk.jfr}
 * module.
 */
public interface EventService {

    private static EventService locateService() {
        EventService svc = null;
        try {
            var s = ServiceLoader.load(EventService.class).findFirst();
            if (s.isPresent()) {
                svc = s.get();
            }
        } catch (ServiceConfigurationError e) {
            e.printStackTrace();
        } finally {
            if (svc == null) {
                svc = new EventService() { };
            }
        }
        return svc;
    }

    /**
     * The service to use for all event logging.  If a service provider
     * cannot be located (the {@jdk.jfr} module not loaded), a default
     * stub service is provided that performs no logging of events.
     */
    static final EventService service = locateService();

    /**
     *
     * @return the object to use to publish network events for socket write
     *   operations.  The default object will not attempt to log any events.
     */
    default SocketReadPublisher socketRead() { return SocketReadPublisher.noPublish; }

    /**
     *
     * @return the object to use to publish network events for socket read
     *   operations.  The default object will not attempt to log any events.
     */
    default SocketWritePublisher socketWrite() { return SocketWritePublisher.noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram send
     *   operations.  The default object will not attempt to log any events.
     */
    default DatagramSendPublisher datagramSend() { return DatagramSendPublisher.noPublish; }

    /**
     *
     * @return the object to use to publish network events for datagram receive
     *   operations.  The default object will not attempt to log any events.
     */
    default DatagramReceivePublisher datagramReceive() { return DatagramReceivePublisher.noPublish; }
}
