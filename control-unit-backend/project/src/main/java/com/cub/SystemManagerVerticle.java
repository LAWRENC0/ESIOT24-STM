package com.cub;

import com.cub.constants.EventBusAddress;
import com.cub.states.TemperatureCUFSM;
import com.cub.states.WindowCUFSM;

import io.vertx.core.AbstractVerticle;
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

        // INCOMING TEMPERATURE UPDATES (from TMS(mqtt))-> triggers the TempCUFSM, which
        // determines
        // OUTGOING updates in temp, freq, angle. angle is only updated if windowCUFSM
        // is in auto mode
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.TEMP), message -> {
            float temperature = (Float) message.body();
            // System.out.println("Received temperature: " + temperature);

            // Process temperature data (determine state, thresholds, etc.)
            JsonObject tempCommand = new JsonObject();
            tempCommand.put("temperature", temperature);
            JsonObject comm = tempCUFSM.handleEvent(tempCommand);
            // in MANUAL mode the ANGLE must NOT be updated by the TempCUFSM
            if (comm.containsKey("temperature"))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.TEMP),
                        comm.getFloat("temperature"));
            if (comm.containsKey("frequency"))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.FREQ),
                        comm.getLong("frequency"));
            if (comm.containsKey("angle") && windowCUFSM.getState() == WindowCUFSM.State.AUTOMATIC)
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE),
                        comm.getInteger("angle"));
        });

        // INCOMING window_state updates (from WCS(Serial), DSHB(http))->triggers the
        // windowCUFSM, which dtermines an update in window_state
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE), message -> {
            String window_state = (String) message.body();
            // System.out.println("Received temperature: " + temperature);

            // Process temperature data (determine state, thresholds, etc.)
            JsonObject windowCommand = new JsonObject();
            windowCommand.put("window_state", window_state);
            JsonObject comm = windowCUFSM.handleEvent(windowCommand);
            if (comm.containsKey("window_state"))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.WINDOW_STATE),
                        comm.getString("window_state"));
        });

        // incoming syst_state updates (from DSHB(http))-> triggers the tempCUFSM which
        // solves the alarm state and sends this update
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.SYSTEM_STATE), message -> {
            String system_state = (String) message.body();

            JsonObject ssCommand = new JsonObject();
            ssCommand.put("system_state", system_state);
            JsonObject comm = tempCUFSM.handleEvent(ssCommand);
            if (comm.containsKey("system_state"))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.SYSTEM_STATE),
                        comm.getValue("system_state"));
        });

        // incoming window_angle updates (from DSHB(http))-> triggers the windowCUFSM
        // which if is in manual mode determines an update in angle
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.ANGLE), message -> {
            int angle = (int) message.body();

            JsonObject angleCommand = new JsonObject();
            angleCommand.put("angle", angle);
            JsonObject comm = windowCUFSM.handleEvent(angleCommand);
            if (comm.containsKey("angle"))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE),
                        comm.getInteger("angle"));
        });
    }

}
