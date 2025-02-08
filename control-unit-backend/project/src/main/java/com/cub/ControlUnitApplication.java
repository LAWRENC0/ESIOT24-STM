package com.cub;

import io.vertx.core.Vertx;

public class ControlUnitApplication {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Deploy all application verticles
        vertx.deployVerticle(new SystemManagerVerticle());
        vertx.deployVerticle(new MqttAgentVerticle());
        vertx.deployVerticle(new SerialAgentVerticle());
        vertx.deployVerticle(new HttpAgentVerticle());

    }
}