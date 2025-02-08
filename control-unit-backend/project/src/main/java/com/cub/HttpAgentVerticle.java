package com.cub;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HttpAgentVerticle extends AbstractVerticle {

    private JsonObject currentState = new JsonObject();

    @Override
    public void start() {
        Router router = Router.router(vertx);

        // Endpoint for the dashboard to fetch the current state
        router.get("/state").handler(this::handleStateRequest);

        vertx.createHttpServer().requestHandler(router).listen(8080, result -> {
            if (result.succeeded()) {
                System.out.println("HTTP server started on port 8080");
            }
        });

        // Listen for dashboard update events
        vertx.eventBus().consumer("dashboard.updates", message -> {
            JsonObject update = (JsonObject) message.body();
            System.out.println("Dashboard update received: " + update);
            // Merge the new update with the current state
            currentState.mergeIn(update);
        });
    }

    private void handleStateRequest(RoutingContext context) {
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(currentState.encode());
    }
}
