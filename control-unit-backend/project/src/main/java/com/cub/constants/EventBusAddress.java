package com.cub.constants;

public enum EventBusAddress {
    TEMP_ADDRESS("temperature"),
    ANGLE_ADDRESS("angle"),
    WCU_ADDRESS("window_control_unit"),
    TCU_ADDRESS("temperature_control_unit");

    private final String address;

    EventBusAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
