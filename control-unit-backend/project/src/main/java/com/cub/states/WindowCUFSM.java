package com.cub.states;

import com.cub.HttpAgentVerticle;
import com.cub.MqttAgentVerticle;
import com.cub.SerialAgentVerticle;
import io.vertx.core.eventbus.EventBus;

public class WindowCUFSM implements ControlUnitFSM<WindowCUFSM.State> {
    public enum State {
        AUTOMATIC("Automatic"), MANUAL("Manual");

        private final String description;

        // Constructor to associate a string with each state
        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private State currentState;
    private final MqttAgentVerticle mqttAgent;
    private final HttpAgentVerticle httpAgent;
    private final SerialAgentVerticle serialAgent;
    private final EventBus eb;

    public WindowCUFSM(MqttAgentVerticle mAg, HttpAgentVerticle hAg, SerialAgentVerticle sAg, EventBus e) {
        this.currentState = State.AUTOMATIC;
        this.mqttAgent = mAg;
        this.httpAgent = hAg;
        this.serialAgent = sAg;
        this.eb = e;
    }

    public State getState() {
        return currentState;
    }

    public void setState(State newState) {
        this.currentState = newState;
    }

    public void handleEvent(String event) {
        switch (currentState) {
            case AUTOMATIC:
                System.out.println("System is off");
                break;
            case MANUAL:
                System.out.println("Cooling system active");
                break;
        }
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }
}
