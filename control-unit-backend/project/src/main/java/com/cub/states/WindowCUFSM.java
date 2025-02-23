package com.cub.states;

import java.util.Objects;

import io.vertx.core.json.JsonObject;

public class WindowCUFSM implements ControlUnitFSM<WindowCUFSM.State> {
    public enum State {
        AUTOMATIC("automatic"), MANUAL("manual");

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

    public WindowCUFSM() {
        this.currentState = State.AUTOMATIC;
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
            if (!(Objects.equals(new_state, State.MANUAL.getDescription()))
                    && !Objects.equals(new_state, State.AUTOMATIC.getDescription())) {
                return new JsonObject();
            }
            switch (currentState) {
                case AUTOMATIC:
                    if (Objects.equals(new_state, State.MANUAL.getDescription())) {
                        setState(State.MANUAL);
                    }
                    break;
                case MANUAL:
                    if (Objects.equals(new_state, State.AUTOMATIC.getDescription())) {
                        setState(State.AUTOMATIC);
                    }
                    break;
            }
            JsonObject message = new JsonObject();
            message.put("window_state",
                    this.getState().getDescription());
            return message;
        } else if (command.containsKey("angle")) {
            int angle = command.getInteger("angle");
            this.door_angle = angle;
            JsonObject message = new JsonObject();
            message.put("angle", this.door_angle);
            return message;
        } else {
            return new JsonObject();
        }
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }
}
