package com.cub;

// In your MQTT agent Verticle
import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;

public class MqttAgentVerticle extends AbstractVerticle {

    @Override
    public void start() {
        MqttClient client = MqttClient.create(vertx);
        client.connect(1883, "mqtt-broker-address", s -> {
            if (s.succeeded()) {
                System.out.println("Connected to MQTT broker");
                client.subscribe("temperature/sensor", 0);
            } else {
                System.err.println("Failed to connect to MQTT broker");
            }
        });

        client.publishHandler(message -> {
            if ("temperature/sensor".equals(message.topicName())) {
                double temperature = Double.parseDouble(message.payload().toString());
                // Publish the temperature update to the system manager
                vertx.eventBus().publish("temperature.updates", temperature);
            }
        });
    }
}
