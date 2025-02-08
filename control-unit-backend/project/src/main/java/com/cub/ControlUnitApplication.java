package com.cub;

import io.vertx.core.Vertx;

public class ControlUnitApplication {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Deploy all application verticles
        vertx.deployVerticle(new SystemManagerVerticle());
    }
}