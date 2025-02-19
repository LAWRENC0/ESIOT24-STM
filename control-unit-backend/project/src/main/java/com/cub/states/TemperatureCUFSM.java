package com.cub.states;

import com.cub.utilities.TemperatureRecord;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class TemperatureCUFSM implements ControlUnitFSM<TemperatureCUFSM.State> {
    public static final float T1_celsius = 20;
    public static final float T2_celsius = 25;
    public static final long DT_ms = 5000;
    public static final int F1_tps = 1; // times per second
    public static final int F2_tps = 2;
    public static final int WINDOW_CLOSED_ANGLE = 0;
    public static final int WINDOW_OPEN_ANGLE = 90;
    private static final int N = 15;

    public enum State {
        NORMAL("Normal"), HOT("Hot"), TOO_HOT("Too_Hot"), ALARM("Alarm");

        private final String description;

        // Constructor to associate a string with each state
        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final EventBus eb;
    private long ts;
    private TemperatureRecord temp_record;
    private State currentState;

    public TemperatureCUFSM(EventBus e) {
        this.currentState = State.NORMAL;
        this.eb = e;
        this.ts = System.currentTimeMillis();
        this.temp_record = new TemperatureRecord(N);
    }

    public State getState() {
        return currentState;
    }

    public void setState(State newState) {
        if (newState == State.TOO_HOT && currentState != State.TOO_HOT) {
            this.ts = System.currentTimeMillis();
        }
        this.currentState = newState;
    }

    public JsonObject handleEvent(JsonObject command) {
        if (command.containsKey("temperature")) {
            float temp = command.getFloat("temperature");
            temp_record.addTemperature(temp);
            switch (currentState) {
                case NORMAL:
                    if (T1_celsius <= temp && T2_celsius >= temp) {
                        setState(State.HOT);
                    }
                    break;
                case HOT:
                    if (T2_celsius < temp) {
                        setState(State.TOO_HOT);
                    } else if (temp < T1_celsius) {
                        setState(State.NORMAL);
                    }
                    break;
                case TOO_HOT:
                    if (T1_celsius <= temp && T2_celsius >= temp) {
                        setState(State.HOT);
                    } else if (System.currentTimeMillis() - this.ts >= DT_ms) {
                        setState(State.ALARM);
                    }
                    break;
                case ALARM:
                    break;
                default:
                    System.out.println("State not found");
                    break;
            }
        } else if (command.containsKey("system_state") && command.getString("system_state") == "normal") {
            switch (currentState) {
                case NORMAL, HOT, TOO_HOT:
                    return new JsonObject();
                case ALARM:
                    setState(State.NORMAL);
                    return command;
            }
        }
        return tick();
    }

    private JsonObject tick() {
        long frequency = 0;
        int angle = WINDOW_CLOSED_ANGLE;
        switch (currentState) {
            case NORMAL:
                frequency = F1_tps;
                angle = WINDOW_CLOSED_ANGLE;
                break;
            case HOT:
                frequency = F2_tps;
                angle = (int) Math
                        .round((0.99 / (T2_celsius - T1_celsius)) * (temp_record.getLastTemperature() - T1_celsius)
                                + 0.01);
                break;
            case TOO_HOT:
                frequency = F2_tps;
                angle = WINDOW_OPEN_ANGLE;
                break;
            case ALARM:
                frequency = F2_tps;
                angle = WINDOW_OPEN_ANGLE;
                break;
        }
        JsonObject message = new JsonObject();
        message.put("frequency", frequency);
        message.put("angle", angle);
        message.put("temperature",
                temp_record.getLastTemperature());
        return message;
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }
}
