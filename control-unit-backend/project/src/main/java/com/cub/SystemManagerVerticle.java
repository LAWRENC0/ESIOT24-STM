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
        this.tempCUFSM = new TemperatureCUFSM();
        this.windowCUFSM = new WindowCUFSM();

        // INCOMING TEMPERATURE UPDATES (from TMS(mqtt))-> triggers the TempCUFSM, which
        // determines
        // OUTGOING updates in temp, freq, angle. angle is only updated if windowCUFSM
        // is in auto mode
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.TEMP), message -> {
            float temperature = (Float) message.body();
            JsonObject tempCommand = new JsonObject();
            tempCommand.put(EventBusAddress.TEMP.getAddress(), temperature);
            JsonObject comm = tempCUFSM.handleEvent(tempCommand);
            // in MANUAL mode the ANGLE must NOT be updated by the TempCUFSM
            if (comm.containsKey(EventBusAddress.TEMP.getAddress())) {
                if (windowCUFSM.getState() == WindowCUFSM.State.MANUAL) {
                    eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.TEMP),
                            comm.getFloat(EventBusAddress.TEMP.getAddress()));
                } else if (windowCUFSM.getState() == WindowCUFSM.State.AUTOMATIC) {
                    eb.publish(EventBusAddress.concat(EventBusAddress.DASHBOARD, EventBusAddress.TEMP),
                            comm.getFloat(EventBusAddress.TEMP.getAddress()));
                }
            }
            if (comm.containsKey(EventBusAddress.FREQ.getAddress()))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.FREQ),
                        comm.getInteger(EventBusAddress.FREQ.getAddress()));
            if (comm.containsKey(EventBusAddress.ANGLE.getAddress())
                    && windowCUFSM.getState() == WindowCUFSM.State.AUTOMATIC) {
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE),
                        comm.getInteger(EventBusAddress.ANGLE.getAddress()));
            }
            if (comm.containsKey(EventBusAddress.SYSTEM_STATE.getAddress()))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.SYSTEM_STATE),
                        comm.getString(EventBusAddress.SYSTEM_STATE.getAddress()));
        });

        // INCOMING window_state updates (from WCS(Serial), DSHB(http))->triggers the
        // windowCUFSM, which dtermines an update in window_state
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE), message ->

        {
            String window_state = (String) message.body();
            JsonObject windowCommand = new JsonObject();
            windowCommand.put(EventBusAddress.WINDOW_STATE.getAddress(), window_state);
            JsonObject comm = windowCUFSM.handleEvent(windowCommand);
            if (comm.containsKey(EventBusAddress.WINDOW_STATE.getAddress())) {
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.WINDOW_STATE),
                        comm.getString(EventBusAddress.WINDOW_STATE.getAddress()));
            }
        });

        // incoming syst_state updates (from DSHB(http))-> triggers the tempCUFSM which
        // solves the alarm state and sends this update
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.SYSTEM_STATE), message -> {
            String system_state = (String) message.body();
            JsonObject ssCommand = new JsonObject();
            ssCommand.put(EventBusAddress.SYSTEM_STATE.getAddress(), system_state);
            JsonObject comm = tempCUFSM.handleEvent(ssCommand);
            if (comm.containsKey(EventBusAddress.SYSTEM_STATE.getAddress())) {
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.SYSTEM_STATE),
                        comm.getValue(EventBusAddress.SYSTEM_STATE.getAddress()));
            }
        });

        // incoming window_angle updates (from DSHB(http))-> triggers the windowCUFSM
        // which if is in manual mode determines an update in angle
        eb.consumer(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.ANGLE), message -> {
            int angle = (int) message.body();
            JsonObject angleCommand = new JsonObject();
            angleCommand.put(EventBusAddress.ANGLE.getAddress(), angle);
            JsonObject comm = windowCUFSM.handleEvent(angleCommand);
            if (comm.containsKey(EventBusAddress.ANGLE.getAddress()))
                eb.publish(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE),
                        comm.getInteger(EventBusAddress.ANGLE.getAddress()));
        });
    }

}
