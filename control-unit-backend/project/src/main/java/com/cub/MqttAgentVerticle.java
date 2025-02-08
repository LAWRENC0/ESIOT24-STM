package com.cub;

import io.netty.handler.codec.mqtt.MqttQoS;
// In your MQTT agent Verticle
import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.core.buffer.Buffer;
import com.cub.constants.EventBusAddress;

public class MqttAgentVerticle extends AbstractVerticle {

    private static final String BROKER = "broker.mqtt-dashboard.com";
    private static final int MQTT_PORT = 1883;
    private final MqttClient client;
    public static final String TEMP_TOPIC = "LAWRENC0-STM/temperature";
    public static final String FREQ_TOPIC = "LAWRENC0-STM/frequency";

    public MqttAgentVerticle() {
        this.client = MqttClient.create(vertx);
    }

    @Override
    public void start() {
        client.connect(MQTT_PORT, BROKER, s -> {
            if (s.succeeded()) {
                System.out.println("Connected to MQTT broker");
                client.subscribe(TEMP_TOPIC, 0);
                client.subscribe(FREQ_TOPIC, 0);
            } else {
                System.err.println("Failed to connect to MQTT broker");
            }
        });

        client.publishHandler(message -> {
            if (TEMP_TOPIC.equals(message.topicName())) {
                double temperature = Double.parseDouble(message.payload().toString());
                // Publish the temperature update to the system manager
                vertx.eventBus().publish(EventBusAddress.TEMP_ADDRESS.getAddress(), temperature);
            }
        });
    }

    public void sendFreqUpdate(String message) {
        if (client != null && client.isConnected()) {
            client.publish(
                    FREQ_TOPIC,
                    Buffer.buffer(message),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    ack -> {
                        if (ack.succeeded()) {
                            System.out.println("Message published to topic: " + FREQ_TOPIC);
                        } else {
                            System.out.println("Failed to publish message: " + ack.cause().getMessage());
                        }
                    });
        } else {
            System.out.println("MQTT client is not connected!");
        }
    }
}
