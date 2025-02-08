package com.cub.states;

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

    public WindowCUFSM() {
        this.currentState = State.AUTOMATIC;
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
