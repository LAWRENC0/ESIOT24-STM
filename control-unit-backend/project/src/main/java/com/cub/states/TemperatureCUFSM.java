package com.cub.states;

import java.util.Objects;

import com.cub.constants.EventBusAddress;
import com.cub.utilities.TemperatureRecord;

import io.vertx.core.json.JsonObject;

public class TemperatureCUFSM implements ControlUnitFSM<TemperatureCUFSM.State> {
    public static final float T1_celsius = 25;
    public static final float T2_celsius = 35;
    public static final long DT_ms = 5000;
    public static final int F1_tpm = 6; // times per minute
    public static final int F2_tpm = 12;
    public static final int WINDOW_CLOSED_ANGLE = 0;
    public static final int WINDOW_OPEN_ANGLE = 90;
    private static final int N = 15;

    public enum State {
        NORMAL("normal"), HOT("hot"), TOO_HOT("too_hot"), ALARM("alarm");

        private final String description;

        // Constructor to associate a string with each state
        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private long ts;
    private int angle;
    private TemperatureRecord temp_record;
    private State currentState;

    public TemperatureCUFSM() {
        this.currentState = State.NORMAL;
        this.ts = System.currentTimeMillis();
        this.temp_record = new TemperatureRecord(N);
        this.angle = WINDOW_CLOSED_ANGLE;
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
        if (command.containsKey(EventBusAddress.TEMP.getAddress())) {
            float temp = command.getFloat(EventBusAddress.TEMP.getAddress());
            temp_record.addTemperature(temp);
            switch (currentState) {
                case NORMAL:
                    if (T1_celsius <= temp && T2_celsius >= temp) {
                        setState(State.HOT);
                    } else if (T2_celsius <= temp) {
                        setState(State.TOO_HOT);
                    }
                    break;
                case HOT:
                    if (T2_celsius <= temp) {
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
                    } else if (temp < T1_celsius) {
                        setState(State.NORMAL);
                    }
                    break;
                case ALARM:
                    break;
                default:
                    System.out.println("State not found");
                    break;
            }
        } else if (command.containsKey(EventBusAddress.SYSTEM_STATE.getAddress())
                && Objects.equals(command.getString(EventBusAddress.SYSTEM_STATE.getAddress()),
                        State.NORMAL.getDescription())) {
            switch (currentState) {
                case NORMAL, HOT, TOO_HOT:
                    return new JsonObject();
                case ALARM:
                    setState(State.NORMAL);
                    return new JsonObject().put(EventBusAddress.SYSTEM_STATE.getAddress(),
                            this.getState().getDescription());
            }
        }
        return tick();
    }

    private JsonObject tick() {
        long frequency = 0;
        switch (currentState) {
            case NORMAL:
                frequency = F1_tpm;
                angle = WINDOW_CLOSED_ANGLE;
                break;
            case HOT:
                frequency = F2_tpm;
                angle = (int) Math
                        .round(((0.99 / (T2_celsius - T1_celsius)) * (temp_record.getLastTemperature() - T1_celsius)
                                + 0.01) * WINDOW_OPEN_ANGLE);
                ;
                break;
            case TOO_HOT:
                frequency = F2_tpm;
                angle = WINDOW_OPEN_ANGLE;
                break;
            case ALARM:
                frequency = F2_tpm;
                angle = WINDOW_OPEN_ANGLE;
                break;
        }
        JsonObject message = new JsonObject();
        message.put(EventBusAddress.FREQ.getAddress(), frequency);
        message.put(EventBusAddress.ANGLE.getAddress(), angle);
        message.put(EventBusAddress.TEMP.getAddress(),
                temp_record.getLastTemperature());
        message.put(EventBusAddress.SYSTEM_STATE.getAddress(), this.getState().getDescription());
        return message;
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }

    public int getAngle() {
        return this.angle;
    }
}
