package com.cub;

import com.fazecast.jSerialComm.SerialPort;
import io.vertx.core.AbstractVerticle;
import com.cub.constants.EventBusAddress;

public class SerialAgentVerticle extends AbstractVerticle {

    private static final int BAUD_RATE = 115200;
    private static final String COMM_PORT = "COM8";
    private SerialPort serialPort;

    @Override
    public void start() {
        // Open your serial port (adjust port name and settings as needed)
        serialPort = SerialPort.getCommPort(COMM_PORT);
        serialPort.setBaudRate(BAUD_RATE);
        if (!serialPort.openPort()) {
            System.err.println("Failed to open serial port");
            return;
        }

        // Optionally, read data from the serial port periodically
        vertx.setPeriodic(1000, id -> {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
                String serialResponse = new String(buffer);
                // Publish serial data for further processing
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.WINDOW_STATE, EventBusAddress.INCOMING),
                        serialResponse);
            }
        });
    }

    @Override
    public void stop() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public void sendMessage(String message) {
        System.out.println("Received serial message to be sent: " + message);
        // Send command to the hardware via the serial port
        serialPort.writeBytes(message.getBytes(), message.length());
    }
}
