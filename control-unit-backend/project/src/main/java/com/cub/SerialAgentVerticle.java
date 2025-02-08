package com.cub;

import com.fazecast.jSerialComm.SerialPort;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class SerialAgentVerticle extends AbstractVerticle {

    private SerialPort serialPort;

    @Override
    public void start() {
        // Open your serial port (adjust port name and settings as needed)
        serialPort = SerialPort.getCommPort("COM3");
        serialPort.setBaudRate(9600);
        if (!serialPort.openPort()) {
            System.err.println("Failed to open serial port");
            return;
        }

        // Listen for commands from the system manager
        vertx.eventBus().consumer("window.controller.commands", message -> {
            JsonObject command = (JsonObject) message.body();
            System.out.println("Received serial command: " + command);
            // Send command to the hardware via the serial port
            String commandString = command.encode();
            serialPort.writeBytes(commandString.getBytes(), commandString.length());
        });

        // Optionally, read data from the serial port periodically
        vertx.setPeriodic(1000, id -> {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
                String serialResponse = new String(buffer);
                // Publish serial data for further processing
                vertx.eventBus().publish("serial.data", new JsonObject().put("response", serialResponse));
            }
        });
    }

    @Override
    public void stop() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
