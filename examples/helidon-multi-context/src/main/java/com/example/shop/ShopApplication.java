package com.example.shop;

import com.example.shop.configuration.ShopConfiguration;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;

public final class ShopApplication {

    private ShopApplication() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        // System properties, then environment variables (DB_CONNECTION_URL...), then application.yaml.
        var application = ShopConfiguration.start(Config.create());
        // Not closed here: on SIGTERM, the server's own shutdown hook stops it and closes the resources.
        System.out.println("Shop is up: http://localhost:" + application.port() + "/orders");
    }
}
