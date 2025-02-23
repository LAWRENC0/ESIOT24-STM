package com.cub;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.JsonSyntaxException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import com.cub.constants.EventBusAddress;
import com.google.gson.JsonParser;

public class SerialAgentVerticle extends AbstractVerticle {

    private static final int BAUD_RATE = 9600;
    private static final String COMM_PORT = "COM7";
    private SerialPort serialPort;

    @Override
    public void start() {
        // Open your serial port (adjust port name and settings as needed)
        serialPort = SerialPort.getCommPort(COMM_PORT);
        serialPort.setBaudRate(BAUD_RATE);
        if (!serialPort.openPort()) {
            System.err.println("Failed to open serial port");
            return;
        } else {
            System.out.println("Opened serial port: " + COMM_PORT);
        }

        // Optionally, read data from the serial port periodically
        vertx.setPeriodic(300, id -> {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
                String serialResponse = new String(buffer).trim();
                System.out.println("REC: " + serialResponse);
                if (!serialResponse.isEmpty() && isValidJson(serialResponse)) {
                    try {
                        JsonObject response = new JsonObject(serialResponse);
                        if (response.containsKey(EventBusAddress.WINDOW_STATE.getAddress())) {
                            vertx.eventBus().publish(
                                    EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE),
                                    response.getString(EventBusAddress.WINDOW_STATE.getAddress()));
                        } else if (response.containsKey(EventBusAddress.ANGLE.getAddress())) {
                            vertx.eventBus().publish(
                                    EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.ANGLE),
                                    response.getInteger(EventBusAddress.ANGLE.getAddress()));
                        }
                    } catch (DecodeException e) {
                        System.out.println("Decode Exception in SerialAgent");
                    }
                }
            }
        });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.TEMP), message -> {
            float temp = (float) message.body();
            String toSend = EventBusAddress.TEMP.getAddress() + ":" + temp + "\n";
            serialPort.writeBytes(toSend.getBytes(), toSend.length());
            // System.out.println(tempMessage);
        });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE), message -> {
            int angle = (int) message.body();
            String toSend = EventBusAddress.ANGLE.getAddress() + ":" + angle + "\n";
            serialPort.writeBytes(toSend.getBytes(), toSend.length());
            // System.out.println(tempMessage);
        });

    }

    @Override
    public void stop() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public static boolean isValidJson(String json) {
        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}