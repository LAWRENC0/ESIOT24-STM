package com.cub.states;

public class TemperatureCUFSM implements ControlUnitFSM<TemperatureCUFSM.State> {
    public enum State {
        NORMAL("Normal"), HOT("Normal"), TOO_HOT("Normal"), ALARM("Normal");

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

    public TemperatureCUFSM() {
        this.currentState = State.NORMAL;
    }

    public State getState() {
        return currentState;
    }

    public void setState(State newState) {
        this.currentState = newState;
    }

    public void handleEvent(String event) {
        switch (currentState) {
            case NORMAL:
                System.out.println("System is off");
                break;
            case HOT:
                System.out.println("Cooling system active");
                break;
            case TOO_HOT:
                System.out.println("Heating system active");
                break;
            case ALARM:
                System.out.println("System idle");
                break;
        }
    }

    public void displayStateMessage() {
        System.out.println("Current state: " + currentState.getDescription());
    }
}
