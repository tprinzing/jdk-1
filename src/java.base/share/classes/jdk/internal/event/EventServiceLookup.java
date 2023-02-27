package jdk.internal.event;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import jdk.internal.misc.VM;

/**
 * Provides the {@link EventService} to use for event logging in {@code java.base}.
 *
 */
public class EventServiceLookup {

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
                // use stub service that does nothing
                svc = init;
            }
        }
        return svc;
    }

    private static synchronized EventService getService() {
        if (service == null) {
            service = locateService();
        }
        return service;
    }

    /**
     * The service to use for all event logging.  If a service provider
     * cannot be located (the {@code jdk.jfr} module not loaded), a default
     * stub service is provided that performs no logging of events.
     */
    public static EventService lookup() {
        if (service != null) {
            return service;
        }
        return (VM.isBooted()) ? getService() : init;
    }

    private static final EventService init = new EventService() { };
    private static volatile EventService service = null;


}
