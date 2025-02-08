package com.cub;

import com.cub.constants.EventBusAddress;
import com.cub.states.ControlUnitFSM;
import com.cub.states.TemperatureCUFSM;
import com.cub.states.WindowCUFSM;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class SystemManagerVerticle extends AbstractVerticle {

    private final ControlUnitFSM tempCUFSM;
    private final ControlUnitFSM windowCUFSM;
    private final MqttAgentVerticle mqttAgent;
    private final HttpAgentVerticle httpAgent;
    private final SerialAgentVerticle serialAgent;
    private final EventBus eb;

    public SystemManagerVerticle() {
        this.eb = vertx.eventBus();
        this.mqttAgent = new MqttAgentVerticle();
        this.httpAgent = new HttpAgentVerticle();
        this.serialAgent = new SerialAgentVerticle();
        vertx.deployVerticle(mqttAgent);
        vertx.deployVerticle(httpAgent);
        vertx.deployVerticle(serialAgent);
        this.tempCUFSM = new TemperatureCUFSM(mqttAgent, httpAgent, serialAgent, eb);
        this.windowCUFSM = new WindowCUFSM(mqttAgent, httpAgent, serialAgent, eb);
    }

    @Override
    public void start() {

        // Listen for temperature updates from MQTT Agent
        eb.consumer(EventBusAddress.TEMP_ADDRESS.getAddress(), message -> {
            double temperature = (Double) message.body();
            System.out.println("Received temperature: " + temperature);

            // Process temperature data (determine state, thresholds, etc.)
            JsonObject windowCommand = processTemperature(temperature);

            // Publish a command to the Serial Agent (Window Controller)
            eb.publish("window.controller.commands", windowCommand);

            // Update dashboard via HTTP Agent
            JsonObject dashboardUpdate = new JsonObject().put("temperature", temperature);
            eb.publish("dashboard.updates", dashboardUpdate);
        });

        // Listen for commands or data from the Serial Agent if needed
        eb.consumer(EventBusAddress.WCU_ADDRESS.getAddress(), message -> {
            // Process serial data and possibly update dashboard or system state
            JsonObject serialData = (JsonObject) message.body();
            System.out.println("Serial data received: " + serialData);
            // For example, update the dashboard:
            eb.publish("dashboard.updates", serialData);
        });

        // You can add more consumers to listen to other events as needed
    }

    private JsonObject processTemperature(double temperature) {
        // Implement your business logic here:
        // For example, if the temperature is too high, command the window to open.
        JsonObject command = new JsonObject();
        if (temperature > 30.0) {
            command.put("action", "open");
        } else {
            command.put("action", "close");
        }
        return command;
    }
}
