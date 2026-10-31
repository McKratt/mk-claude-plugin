package com.example.shop.configuration;

import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRouting;
import lombok.RequiredArgsConstructor;

/**
 * Closes a resource, such as the connection pool, when the web server stops gracefully. Helidon calls
 * {@link #afterStop()} once the server accepts no more connections and the running requests have completed or the
 * shutdown grace period ({@code server.shutdown-grace-period}) has elapsed. The server's shutdown hook, on by
 * default, stops it when the JVM exits.
 */
@RequiredArgsConstructor
final class CloseOnStop implements HttpFeature {

    private static final System.Logger LOGGER = System.getLogger(CloseOnStop.class.getName());

    private final AutoCloseable resource;

    @Override
    public void setup(HttpRouting.Builder routing) {
        // No route: the feature only takes part in the server's lifecycle.
    }

    @Override
    public void afterStop() {
        try {
            resource.close();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to close " + resource, e);
        }
    }
}
