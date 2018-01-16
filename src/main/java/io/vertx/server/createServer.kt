package io.vertx.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router

import java.sql.Timestamp
import java.util.ArrayList
import java.util.Objects

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(createServer())
}

class createServer : AbstractVerticle() {

    private var httpServer: HttpServer? = null
    private val db_name = "data"
    private val table_name = "json_data"
    private val port = 1234

    @Throws(Exception::class)
    override fun start() {
        httpServer = vertx.createHttpServer()

        val router = Router.router(vertx)

        val config = JsonObject()
                .put("connection_string", "mongodb://localhost:27010")
                .put("db_name", db_name)

        val client = MongoClient.createShared(vertx, config)

        router.route().handler { routingContext ->
            val response = routingContext.response()
            val request = routingContext.request()
            if (request.method() == HttpMethod.POST) {
                request.bodyHandler { bodyHandler ->
                    val body = bodyHandler.toJsonObject()
                    val data = body.getString("data")

                    val ts = Timestamp(System.currentTimeMillis())
                    val document = JsonObject().put("data", data).put("time", ts.time)
                    client.insert(table_name, document) { res ->
                        if (res.succeeded()) {
                            val id = res.result()
                            response.putHeader("content-type", "text/plain")
                            response.end("Ok")
                        } else {
                            res.cause().printStackTrace()
                            response.putHeader("content-type", "text/plain")
                            response.end("Error")
                        }
                    }
                }
                //System.out.println("Post request");
            } else if (request.method() == HttpMethod.GET) {
                var min_v: String? = request.getParam("min")
                var max_v: String? = request.getParam("max")
                if (min_v == null) min_v = "0"
                if (max_v == null) {
                    max_v = Objects.toString(Timestamp(System.currentTimeMillis()).time)
                }
                val query = JsonObject().put("time", JsonObject().put("\$gte",
                        java.lang.Long.valueOf(min_v)).put("\$lte",
                        java.lang.Long.valueOf(max_v!!)))

                client.find(table_name, query) { res ->
                    val ans = ArrayList<JsonObject>()
                    if (res.succeeded()) {
                        ans.addAll(res.result())
                    } else {
                        res.cause().printStackTrace()
                    }
                    response.putHeader("content-type", "application/json; charset=utf-8")
                    response.end(Json.encodePrettily(ans))
                }
                //Timestamp ts = new Timestamp(System.currentTimeMillis());
                //System.out.println("Get request " + ts.getTime());
            }
            //response.end("ok");
        }

        httpServer!!.requestHandler(Handler<HttpServerRequest> { router.accept(it) }).listen(port)
    }
}
