package com.cub.states;

import io.vertx.core.json.JsonObject;

public interface ControlUnitFSM<T extends Enum<T>> {

    // Set the current state
    void setState(T state);

    // Get the current state
    T getState();

    // Handle an event to transition or act based on the current state
    void handleEvent(JsonObject command);

    // Display the current state's information (if any)
    void displayStateMessage();
}
