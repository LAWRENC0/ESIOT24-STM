package com.cub;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        MQTTAgent agent = new MQTTAgent();
        agent.init(vertx, context);
        agent.start();

        Router router = Router.router(vertx);

        Route route = router.route();
        route.handler(ctx -> {

            HttpServerResponse response = ctx.response();
            // enable chunked responses because we will be adding data as
            // we execute over other handlers. This is only required once and
            // only if several handlers do output.
            response.setChunked(true);

            response.end("route1\n");
        });

        server.requestHandler(router).listen(8080);
    }
}