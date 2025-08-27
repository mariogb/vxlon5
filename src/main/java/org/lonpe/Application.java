package org.lonpe;

import com.hazelcast.config.Config;
import io.vertx.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Application {

    public static void start() {
        System.out.println("Application started");

        final Config hzConfig = new Config();

        final HazelcastClusterManager hazelcastClusterManager = new HazelcastClusterManager(hzConfig);
        Vertx.builder()
                .withClusterManager(hazelcastClusterManager)
                .buildClustered()
                .onComplete(ar -> {
                    if (ar.succeeded()) {

                        Vertx vertx = ar.result();
                        vertx.deployVerticle(new MainVerticle())
                                .onSuccess(id -> System.out.println("Deployment id is: " + id))
                                .onFailure(err -> System.out.println("Deployment failed! " + err.getMessage()));

                        System.out.println("Clustered Vert.x started");
                    } else {
                        System.out.println("Failed to start clustered Vert.x");
                    }
                });
    }

    public static void main(String[] args) {
        start();

    }

}
