package io.vertx.server;

import io.netty.handler.codec.http.HttpResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class vServer {
    private static final String MONGO_EXAMPLES_DIR = "vertxServer";
    private static final String MONGO_EXAMPLES_JAVA_DIR = MONGO_EXAMPLES_DIR + "/src/main/java/";


    public static void main(String[] args) {
        VertxOptions options = new VertxOptions();

        Class cl = createServer.class;

        String dir = MONGO_EXAMPLES_JAVA_DIR + cl.getPackage().getName().replace(".", "/");
        String verticleID = cl.getName();

        try {
            File current = new File(".").getCanonicalFile();
            if (dir.startsWith(current.getName()) && !dir.equals(current.getName())) {
                dir = dir.substring(current.getName().length() + 1);
            };
        } catch (IOException e) {
            System.out.println("Ignored");
        }

        System.setProperty("vertx.cmd", dir);
        Consumer<Vertx> runner = vertx -> {
            try {
                vertx.deployVerticle(verticleID);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };

        if (options.isClustered()) {
            Vertx.clusteredVertx(options, res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    runner.accept(vertx);
                }
                else {
                    res.cause().printStackTrace();
                }
            });
        } else {
            Vertx vertx = Vertx.vertx(options);
            runner.accept(vertx);
        }

    }
}
