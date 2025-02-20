package com.cub;

import com.cub.constants.EventBusAddress;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.Queue;

public class HttpAgentVerticle extends AbstractVerticle {

    private JsonObject currentState = new JsonObject()
            .put(EventBusAddress.SYSTEM_STATE.getAddress(), "normal")
            .put(EventBusAddress.ANGLE.getAddress(), 40)
            .put(EventBusAddress.WINDOW_STATE.getAddress(), "manual")
            .put("avgTemp", 22.5)
            .put("maxTemp", 25)
            .put("minTemp", 20)
            .put("graph", new JsonObject().put("labels", new JsonArray()).put("temperatures", new JsonArray()));

    private static final int MAX_GRAPH_POINTS = 10;
    private final Queue<Double> temperatureHistory = new LinkedList<>();
    private final Queue<String> timeLabels = new LinkedList<>();

    @Override
    public void start() {
        Router router = Router.router(vertx);

        // Enable request body handling for POST requests
        router.route().handler(BodyHandler.create());

        // Serve static files
        router.route().handler(StaticHandler.create("webroot"));

        // API endpoints
        router.get("/state").handler(this::handleStateRequest);
        router.post("/control").handler(this::handleControlRequest);

        vertx.createHttpServer().requestHandler(router).listen(8080, result -> {
            if (result.succeeded()) {
                System.out.println("HTTP server started on port 8080");
            }
        });

        // Listen for updates from the event bus
        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.SYSTEM_STATE),
                message -> {
                    if (message.body() instanceof JsonObject) {
                        updateSystemState((JsonObject) message.body());
                    }
                });
    }

    // Handle GET /state
    private void handleStateRequest(RoutingContext context) {
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(currentState.encode());
    }

    // Handle POST /control
    private void handleControlRequest(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Invalid request");
            return;
        }
        // SYSTEM STATE
        if (body.containsKey(EventBusAddress.SYSTEM_STATE.getAddress())) {
            String sState = body.getString(EventBusAddress.SYSTEM_STATE.getAddress());
            if (sState == "normal") {
                currentState.put(EventBusAddress.SYSTEM_STATE.getAddress(), sState);
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.SYSTEM_STATE),
                        sState);
            } else {
                System.out.println("Invalid value for system_state received from dashboard");
            }
        }
        // WINDOW_STATE
        if (body.containsKey(EventBusAddress.WINDOW_STATE.getAddress())) {
            String wState = body.getString(EventBusAddress.WINDOW_STATE.getAddress());
            if (wState == "automatic" || wState == "manual") {
                currentState.put(EventBusAddress.WINDOW_STATE.getAddress(), wState);
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE),
                        wState);
            } else {
                System.out.println("Invalid value for system_state received from dashboard");
            }
        }
        // ANGLE
        if (body.containsKey(EventBusAddress.ANGLE.getAddress())) {
            String response = body.getString(EventBusAddress.ANGLE.getAddress());
            try {
                int angle = Integer.parseInt(response);
                currentState.put(EventBusAddress.ANGLE.getAddress(), angle);
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.ANGLE),
                        angle);
            } catch (NumberFormatException e) {
                System.out.println("Unparsable value received from dashboard as angle value");
            }
        }

        context.response().setStatusCode(200).end("Action processed");
    }

    // Update system state when receiving event bus messages
    private void updateSystemState(JsonObject newState) {
        currentState.mergeIn(newState);

        // Update graph with new temperature if available
        if (newState.containsKey("temperature")) {
            double temp = newState.getDouble("temperature");
            addTemperatureData(temp);
        }
    }

    // Add temperature data to the graph history
    private void addTemperatureData(double temperature) {
        if (temperatureHistory.size() >= MAX_GRAPH_POINTS) {
            temperatureHistory.poll();
            timeLabels.poll();
        }
        temperatureHistory.add(temperature);
        timeLabels.add(LocalTime.now().toString().substring(0, 5)); // HH:mm format

        currentState.put("avgTemp", temperatureHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        currentState.put("maxTemp", temperatureHistory.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        currentState.put("minTemp", temperatureHistory.stream().mapToDouble(Double::doubleValue).min().orElse(0));

        currentState.put("graph", new JsonObject()
                .put("labels", new JsonArray(new LinkedList<>(timeLabels)))
                .put("temperatures", new JsonArray(new LinkedList<>(temperatureHistory))));
    }
}
