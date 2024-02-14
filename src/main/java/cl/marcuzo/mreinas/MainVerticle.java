package cl.marcuzo.mreinas;

import cl.marcuzo.mreinas.verticle.OrquestadorVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Vertx vertx = Vertx.vertx();
        JsonObject config = new JsonObject().put("N", 8); // Asumiendo que queremos resolver para N=8
        DeploymentOptions options = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle(OrquestadorVerticle.class.getName(), options, res -> {
            if (res.succeeded()) {
                System.out.println("Orquestador desplegado correctamente");
            } else {
                System.out.println("Fallo al desplegar el orquestador");
            }
        });
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
