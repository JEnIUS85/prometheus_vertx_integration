package com.sample.vertx.service.verticle;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;


public class RouterClass extends AbstractVerticle {

    @Override
    public void start(Future<Void> future){

        Router router = Router.router(vertx);
        router.get("/router/:id")
                .handler(this::routerMethod);

        router.get("/")
                .handler(this::HelloWorld);

        /************Expose Metrics to Promethes **************/
        PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
        registry
                .config()
                .meterFilter(
                        new MeterFilter() {
                            @Override
                            public DistributionStatisticConfig configure(
                                    Meter.Id id, DistributionStatisticConfig config) {
                                return DistributionStatisticConfig.builder()
                                        .percentilesHistogram(true)
                                        .build()
                                        .merge(config);
                            }
                        }
                );

        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);

        router.route(VertxPrometheusOptions.DEFAULT_EMBEDDED_SERVER_ENDPOINT).handler(ctx -> {
            String response = registry.scrape();
            ctx.response().end(response);
        });

        /**************************************/
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        });
    }

    private void routerMethod(RoutingContext routingContext) {
        String articleId = routingContext.request()
                .getParam("id");


        routingContext.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end("You have called Router with Id : "+ articleId);
    }

    private void HelloWorld(RoutingContext routingContext){
        routingContext.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end("Welcome to Vert.x Intro -- RouterClass");
    }


}
