package com.example.shop.configuration;

import io.helidon.webserver.WebServer;

/** The started application, as returned by {@link ShopConfiguration#start}. Closing it stops the server gracefully. */
public final class RunningApplication implements AutoCloseable {

    private final WebServer server;

    RunningApplication(WebServer server) {
        this.server = server;
    }

    /** The port the server listens on, which is the actual one when {@code server.port} is 0. */
    public int port() {
        return server.port();
    }

    /** Stops the server, which then closes the application's resources through {@link CloseOnStop}. */
    @Override
    public void close() {
        server.stop();
    }
}
