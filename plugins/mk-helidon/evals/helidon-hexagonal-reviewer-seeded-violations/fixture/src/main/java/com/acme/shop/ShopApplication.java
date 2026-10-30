package com.acme.shop;

import com.acme.shop.configuration.ShopConfiguration;
import io.helidon.config.Config;

public final class ShopApplication {

    private ShopApplication() {
    }

    public static void main(String[] args) {
        ShopConfiguration.start(Config.create());
    }
}
