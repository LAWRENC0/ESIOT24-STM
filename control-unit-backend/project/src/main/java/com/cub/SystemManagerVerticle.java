package com.cub;

import com.cub.constants.EventBusAddress;
import com.cub.states.TemperatureCUFSM;
import com.cub.states.WindowCUFSM;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class SystemManagerVerticle extends AbstractVerticle {

    private TemperatureCUFSM tempCUFSM;
    private WindowCUFSM windowCUFSM;
    private MqttAgentVerticle mqttAgent;
    private HttpAgentVerticle httpAgent;
    private SerialAgentVerticle serialAgent;
    private EventBus eb;

    @Override
    public void start() {
        this.eb = vertx.eventBus();
        this.mqttAgent = new MqttAgentVerticle();
        this.httpAgent = new HttpAgentVerticle();
        this.serialAgent = new SerialAgentVerticle();
        vertx.deployVerticle(mqttAgent);
        vertx.deployVerticle(httpAgent);
        vertx.deployVerticle(serialAgent);
        this.tempCUFSM = new TemperatureCUFSM(eb);
        this.windowCUFSM = new WindowCUFSM(eb);

        // Listen for temperature updates from MQTT Agent
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.TEMP), message -> {
            float temperature = (Float) message.body();
            // System.out.println("Received temperature: " + temperature);

            // Process temperature data (determine state, thresholds, etc.)
            JsonObject tempCommand = new JsonObject();
            tempCommand.put("temperature", temperature);
            tempCUFSM.handleEvent(tempCommand);
        });

        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE), message -> {
            float temperature = (Float) message.body();
            // System.out.println("Received temperature: " + temperature);

            // Process temperature data (determine state, thresholds, etc.)
            JsonObject tempCommand = new JsonObject();
            tempCommand.put("temperature", temperature);
            tempCUFSM.handleEvent(tempCommand);
        });
    }

}
