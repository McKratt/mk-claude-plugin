package com.example.shop;

import com.example.shop.configuration.ShopConfiguration;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;

public final class ShopApplication {

    private ShopApplication() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        // System properties, then environment variables (DB_CONNECTION_URL...), then application.yaml.
        var config = Config.create();
        var server = WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> ShopConfiguration.routing(routing, config))
                .build()
                .start();
        System.out.println("Shop is up: http://localhost:" + server.port() + "/orders");
    }
}
