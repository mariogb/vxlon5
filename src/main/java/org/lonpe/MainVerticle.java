package org.lonpe;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.rxjava3.pgclient.PgBuilder;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;

import org.lonpe.db.DBLon5;
import org.lonpe.db.DBLon5Enhanced;

import io.vertx.config.ConfigRetriever;

public class MainVerticle extends VerticleBase {

  private Pool doDbPool;

  @Override
  public Future<?> start() {

    doConfig0().getConfig().onSuccess(conf -> {
      ml("Conf:" + conf.encodePrettily());
      doDbPool = doDbPool(conf);

      // DBLon5 dbLon5 = new DBLon5(doDbPool);
      // dbLon5.doPreparedQuery("SELECT * FROM user_lon where id > $1", Tuple.of(0))
      //     .subscribe(rrs -> {
      //       ml("RRS size:" + rrs.size());
      //       rrs.forEach(row -> {
      //         ml("Row:" + row.toJson());
      //       });
      //     }, err -> {
      //       ml("Err query:" + err.getMessage());
      //     });

      DBLon5Enhanced dbLon5Enhanced = new DBLon5Enhanced(doDbPool);

      dbLon5Enhanced.findOne("SELECT * FROM user_lon where id > $1", Tuple.of(0))
      .subscribe(result -> {
      ml("User found: " + result);
      }, error -> {
      ml("Error finding user: " + error.getMessage());
      });

    }).onFailure(err -> {
      ml("Err conf:" + err.getMessage());
    });

    return vertx.createHttpServer().requestHandler(req -> {
      req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!");
    }).listen(8888).onSuccess(http -> {
      System.out.println("HTTP server started on port 8888");
    });
  }

  @Override
  public Future<?> stop() {
    return Future.succeededFuture();
  }

  private ConfigRetriever doConfig0() {
    ml("DoConfig");
    final ConfigStoreOptions fileStore = new ConfigStoreOptions()
        .setType("file")
        .setConfig(new JsonObject().put("path", "config-lon.json"));

    final ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("env");

    final ConfigRetrieverOptions options = new ConfigRetrieverOptions()
        .addStore(fileStore)
        .addStore(sysPropsStore);
    Vertx.currentContext().owner();
    return ConfigRetriever.create(Vertx.currentContext().owner(), options);

  }

  private Pool doDbPool(JsonObject configApp) {

    final PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(configApp.getInteger("POSTGRES_PORT"))
        .setHost(configApp.getString("POSTGRES_HOST"))
        .setDatabase(configApp.getString("POSTGRES_DB"))
        .setUser(configApp.getString("POSTGRES_USER"))
        .setPassword(configApp.getString("POSTGRES_PASSWORD"));

    // Pool options
    final PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(configApp.getInteger("POSTGRES_POOL_MAXSIZE", 8));

    try {
      Pool pool = PgBuilder.pool()
          .with(poolOptions)
          .connectingTo(connectOptions)
          .using(io.vertx.rxjava3.core.Vertx.currentContext().owner())
          .build();

      ml("Pool created successfully");
      return pool;

    } catch (Exception e) {
      ml("Error creating pool: " + e.getMessage());
      throw e;
    }

  }

  private void ml(String string) {

    System.out.println(string);
  }

}
