package com.cub;

import io.netty.handler.codec.mqtt.MqttQoS;
// In your MQTT agent Verticle
import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.core.buffer.Buffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cub.constants.EventBusAddress;

public class MqttAgentVerticle extends AbstractVerticle {

    private static final String BROKER = "broker.mqtt-dashboard.com";
    private static final int MQTT_PORT = 1883;
    private MqttClient client;
    public static final String TEMP_TOPIC = "LAWRENC0-STM/temperature";
    public static final String FREQ_TOPIC = "LAWRENC0-STM/frequency";

    @Override
    public void start() {
        this.client = MqttClient.create(vertx);
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
                String m = message.payload().toString();
                Pattern pattern = Pattern.compile("\\d+\\.\\d+");
                Matcher matcher = pattern.matcher(m);

                if (matcher.find()) {
                    float temperature = Float.parseFloat(matcher.group());
                    vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.TEMP),
                            temperature);
                } else {
                    System.out.println("No float value found in the string.");
                }
                // Publish the temperature update to the system manager
            }
        });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.FREQ), message -> {
            if (client != null && client.isConnected()) {
                client.publish(
                        FREQ_TOPIC,
                        Buffer.buffer(String.valueOf((Long) message.body())),
                        MqttQoS.AT_LEAST_ONCE,
                        false,
                        false,
                        ack -> {
                            if (ack.succeeded()) {
                                // System.out.println("Message published to topic: " + FREQ_TOPIC);
                            } else {
                                System.out.println("Failed to publish message: " + ack.cause().getMessage());
                            }
                        });
            } else {
                System.out.println("MQTT client is not connected!");
            }
        });

    }
}
