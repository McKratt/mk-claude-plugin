package com.example.shop.configuration;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * What the application must release, such as the connection pool, closed in the reverse order of creation. It covers
 * both ends of the application's life: {@link CloseOnStop} closes it when the server stops, and
 * {@link ShopConfiguration#start} closes it when the startup fails, before there is a server to stop.
 */
final class ApplicationResources implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(ApplicationResources.class.getName());

    private final Deque<AutoCloseable> resources = new ConcurrentLinkedDeque<>();

    void add(AutoCloseable resource) {
        resources.addFirst(resource);
    }

    /** Closes each resource once: a failure is logged and does not prevent closing the others. */
    @Override
    public void close() {
        AutoCloseable resource;
        while ((resource = resources.pollFirst()) != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to close " + resource, e);
            }
        }
    }
}
