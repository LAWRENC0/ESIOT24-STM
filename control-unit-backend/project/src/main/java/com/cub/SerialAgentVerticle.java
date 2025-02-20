package com.cub;

import com.fazecast.jSerialComm.SerialPort;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import com.cub.constants.EventBusAddress;

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
        vertx.setPeriodic(50, id -> {
            if (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
                String serialResponse = new String(buffer).trim();
                System.out.println("SERIAL" + serialResponse);
                JsonObject response = new JsonObject().put("window_state", serialResponse);
                // JsonObject response = new JsonObject(serialResponse);
                vertx.eventBus().publish(
                        EventBusAddress.concat(EventBusAddress.INCOMING,
                                EventBusAddress.WINDOW_STATE),
                        response.getValue("window_state"));
            }
        });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.TEMP), message -> {
            float temp = (float) message.body();
            JsonObject tempMessage = new JsonObject().put("temp", temp);
            System.out.println("writing temp to serial: " + tempMessage);
            System.out.println("Bytes written: "
                    + serialPort.writeBytes(tempMessage.toString().getBytes(), tempMessage.toString().length()));
        });

        // vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING,
        // EventBusAddress.ANGLE), message -> {
        // String angle = (String) message.body();
        // serialPort.writeBytes(angle.getBytes(), angle.length());
        // });

        // vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING,
        // EventBusAddress.WINDOW_STATE),
        // message -> {
        // String window_state = (String) message.body();
        // serialPort.writeBytes(window_state.getBytes(), window_state.length());
        // });

    }

    @Override
    public void stop() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
