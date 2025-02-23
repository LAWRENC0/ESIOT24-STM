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
import java.util.Objects;
import java.util.Queue;

public class HttpAgentVerticle extends AbstractVerticle {

    private JsonObject currentState = new JsonObject()
            .put(EventBusAddress.SYSTEM_STATE.getAddress(), "normal")
            .put(EventBusAddress.WINDOW_STATE.getAddress(), "automatic")
            .put("graph", new JsonObject().put("labels", new JsonArray()).put("temperatures", new JsonArray()));

    private static final int MAX_GRAPH_POINTS = 100;
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

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.SYSTEM_STATE),
                message -> {
                    if (message.body() instanceof String) { // Accepts Float, Double, etc.
                        String sState = ((String) message.body());
                        JsonObject sStateJson = new JsonObject().put(EventBusAddress.SYSTEM_STATE.getAddress(), sState);
                        updateSystemState(sStateJson);
                    }
                });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.WINDOW_STATE),
                message -> {
                    if (message.body() instanceof String) { // Accepts Float, Double, etc.
                        String wState = ((String) message.body());
                        JsonObject wStateJson = new JsonObject().put(EventBusAddress.WINDOW_STATE.getAddress(), wState);
                        updateSystemState(wStateJson);
                    }
                });

        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.ANGLE),
                message -> {
                    if (message.body() instanceof Number) { // Accepts Float, Double, etc.
                        float angle = ((Number) message.body()).intValue();
                        JsonObject angleJson = new JsonObject().put(EventBusAddress.ANGLE.getAddress(), angle);
                        updateSystemState(angleJson);
                    }
                });

        // Listen for updates from the event bus
        vertx.eventBus().consumer(EventBusAddress.concat(EventBusAddress.OUTGOING, EventBusAddress.TEMP),
                message -> {
                    if (message.body() instanceof Number) { // Accepts Float, Double, etc.
                        float temperature = ((Number) message.body()).floatValue();
                        JsonObject tempJson = new JsonObject().put(EventBusAddress.TEMP.getAddress(), temperature);
                        updateSystemState(tempJson);
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
            String sState = body.getString(EventBusAddress.SYSTEM_STATE.getAddress()).trim();
            if (Objects.equals(sState, "normal")) {
                currentState.put(EventBusAddress.SYSTEM_STATE.getAddress(), sState);
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.SYSTEM_STATE),
                        sState);
            } else {
                System.out.println("Invalid value for system_state received from dashboard: " + sState);
            }
        }
        // WINDOW_STATE
        if (body.containsKey(EventBusAddress.WINDOW_STATE.getAddress())) {
            String wState = body.getString(EventBusAddress.WINDOW_STATE.getAddress()).trim();
            if (Objects.equals(wState, "automatic") || Objects.equals(wState, "manual")) {
                currentState.put(EventBusAddress.WINDOW_STATE.getAddress(), wState);
                vertx.eventBus().publish(EventBusAddress.concat(EventBusAddress.INCOMING, EventBusAddress.WINDOW_STATE),
                        wState);
            } else {
                System.out.println("Invalid value for system_state received from dashboard: " + wState);
            }
        }
        // ANGLE
        if (body.containsKey(EventBusAddress.ANGLE.getAddress())) {
            String response = body.getString(EventBusAddress.ANGLE.getAddress()).trim();
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
        temperatureHistory.add(Math.floor(temperature * 10) / 10);
        timeLabels.add(LocalTime.now().toString().substring(0, 5)); // HH:mm format

        currentState.put("avgTemp",
                Math.floor(temperatureHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10) / 10);
        currentState.put("maxTemp", temperatureHistory.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        currentState.put("minTemp", temperatureHistory.stream().mapToDouble(Double::doubleValue).min().orElse(0));

        currentState.put("graph", new JsonObject()
                .put("labels", new JsonArray(new LinkedList<>(timeLabels)))
                .put("temperatures", new JsonArray(new LinkedList<>(temperatureHistory))));
    }
}
