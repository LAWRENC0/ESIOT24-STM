package com.cub.states;

import com.cub.constants.EventBusAddress;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import netscape.javascript.JSObject;

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
    private int door_angle;
    private final EventBus eb;

    public WindowCUFSM(EventBus e) {
        this.currentState = State.AUTOMATIC;
        this.eb = e;
    }

    public State getState() {
        return currentState;
    }

    public void setState(State newState) {
        this.currentState = newState;
    }

    public JsonObject handleEvent(JsonObject command) {
        if (command.containsKey("window_state")) {
            String new_state = command.getString("window_state");
            switch (currentState) {
                case AUTOMATIC:
                    if (new_state == State.MANUAL.getDescription()) {
                        setState(State.MANUAL);
                    }
                    break;
                case MANUAL:
                    if (new_state == State.AUTOMATIC.getDescription()) {
                        setState(State.AUTOMATIC);
                    }
                    break;
            }
        } else if (command.containsKey("angle")) {
            int angle = command.getInteger("angle");
            this.door_angle = angle;
        }
        return tick();
    }

    private JsonObject tick() {
        JsonObject message = new JsonObject();
        message.put(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE), this.door_angle);
        message.put(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.WINDOW_STATE),
                this.getState().getDescription());
        return message;
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }
}
