package com.cub.constants;

public enum EventBusAddress {
    TEMP("temperature"),
    FREQ("frequency"),
    WINDOW_STATE("window_state"),
    SYSTEM_STATE("system_state"),
    ANGLE("angle"),
    INCOMING("incoming"),
    OUTGOING("outgoing");

    private final String address;

    EventBusAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public static String concat(EventBusAddress prefix, EventBusAddress suffix) {
        return prefix.getAddress() + "." + suffix.getAddress();
    }
}
