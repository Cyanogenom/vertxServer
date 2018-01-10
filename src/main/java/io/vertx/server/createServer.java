package io.vertx.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class createServer extends AbstractVerticle {

    private HttpServer httpServer = null;
    private String db_name = "data";
    private String table_name = "json_data";
    private Integer port = 1234;

    @Override
    public void start() throws Exception {
        httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

        JsonObject config = new JsonObject()
                .put("connection_string", "mongodb://localhost:27010")
                .put("db_name", db_name);

        MongoClient client = MongoClient.createShared(vertx, config);

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            HttpServerRequest request = routingContext.request();
            if (request.method() == HttpMethod.POST) {
                request.bodyHandler(bodyHandler -> {
                    final JsonObject body = bodyHandler.toJsonObject();
                    String data = body.getString("data");

                    Timestamp ts = new Timestamp(System.currentTimeMillis());
                    JsonObject document = new JsonObject().put("data", data).put("time", ts.getTime());
                    client.insert(table_name, document, res -> {
                        if (res.succeeded()) {
                            String id = res.result();
                            response.putHeader("content-type", "text/plain");
                            response.end("Ok");
                        } else {
                            res.cause().printStackTrace();
                            response.putHeader("content-type", "text/plain");
                            response.end("Error");
                        }
                    });
                });

            }
            else if (request.method() == HttpMethod.GET) {
                String min_v = request.getParam("min");
                String max_v = request.getParam("max");
                if(min_v == null) {
                    min_v = "0";
                }
                if(max_v == null)
                {
                    max_v = Objects.toString(new Timestamp(System.currentTimeMillis()).getTime());
                }
                JsonObject query = new JsonObject().put("time", new JsonObject().put("$gte",
                        Long.valueOf(min_v)).put("$lte",
                        Long.valueOf(max_v)));

                client.find(table_name, query, res -> {
                    ArrayList<JsonObject> ans = new ArrayList<JsonObject>();
                    if (res.succeeded()) {
                        ans.addAll(res.result());
                    } else {
                        res.cause().printStackTrace();
                    }
                    response.putHeader("content-type", "application/json; charset=utf-8");
                    response.end(Json.encodePrettily(ans));
                });

            }
        });

        httpServer.requestHandler(router::accept).listen(port);
    }
}
