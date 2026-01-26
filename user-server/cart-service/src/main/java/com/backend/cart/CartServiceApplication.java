package com.backend.cart;

import com.backend.cart.config.InventoryServiceProperties;
import com.backend.cart.config.OrderServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ OrderServiceProperties.class, InventoryServiceProperties.class })
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
