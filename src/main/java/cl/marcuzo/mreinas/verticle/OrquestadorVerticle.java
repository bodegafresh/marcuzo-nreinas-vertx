package cl.marcuzo.mreinas.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class OrquestadorVerticle extends AbstractVerticle  {

 
    public void start(Future<Void> startFuture) {
        // Recibir N como parámetro de configuración
        int N = config().getInteger("N", 8); // Default a 8 si no se proporciona
        System.out.println("start verticle trabajador");

        // Aquí iría la lógica para desplegar los verticles trabajadores y manejar los resultados
        // Por ejemplo, desplegar un verticle para cada posible posición inicial de la primera reina
        for (int i = 0; i < N; i++) {
            JsonObject config = new JsonObject().put("filaInicial", i).put("N", N);
            vertx.deployVerticle("TuVerticleTrabajador", new DeploymentOptions().setConfig(config), res -> {
                if (res.succeeded()) {
                    String deploymentID = res.result();
                    System.out.println("Desplegado verticle trabajador con ID: " + deploymentID);
                    // Registrar manejadores para recibir soluciones a través del EventBus
                } else {
                    System.out.println("Fallo al desplegar verticle trabajador");
                }
            });
        }

        // Recoger y combinar resultados de los verticles trabajadores
        // Puedes usar el EventBus para recibir los resultados
    }
    
}
