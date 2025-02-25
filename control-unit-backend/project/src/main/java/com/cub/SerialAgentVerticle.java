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
    private StringBuilder serialBuffer = new StringBuilder();

    @Override
    public void start() {
        serialPort = SerialPort.getCommPort(COMM_PORT);
        serialPort.setBaudRate(BAUD_RATE);
        if (!serialPort.openPort()) {
            System.err.println("Failed to open serial port");
            return;
        } else {
            System.out.println("Opened serial port: " + COMM_PORT);
        }

        vertx.setPeriodic(50, id -> {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
                String serialResponse = new String(buffer).trim();
                serialBuffer.append(serialResponse); // Accumulate data
                processSerialBuffer();
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
            System.out.println("a = " + toSend);
        });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.WINDOW_STATE),
                message -> {
                    String wState = (String) message.body();
                    String toSend = EventBusAddress.WINDOW_STATE.getAddress() + ":" + wState + "\n";
                    serialPort.writeBytes(toSend.getBytes(), toSend.length());
                    // System.out.println(tempMessage);
                });

    }

    private void processSerialBuffer() {
        while (serialBuffer.indexOf("}") != -1) { // While there's a complete message
            int endIndex = serialBuffer.indexOf("}");
            String message = serialBuffer.substring(0, endIndex + 1).trim(); // Extract one full message
            serialBuffer.delete(0, endIndex + 1); // Remove processed message from buffer
            System.out.println(message);
            if (!message.isEmpty() && isValidJson(message)) {
                try {
                    JsonObject response = new JsonObject(message);
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
                    System.out.println("Decode Exception in SerialAgent: " + message);
                }
            } else {
                System.out.println("Invalid or Incomplete Message Discarded: " + message);
            }
        }
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