# prometheus_vertx_integration
Sample Repo containing basic code required to monitor existing service metrics through Prometheus
Here I have tried to list down simple steps required to integrate Prometheus monitoring with exsiting vertex application.

Basic Vertx Service Code:
a) com.sample.vertx.service.MainClass ==>
        Main Class that create vertex object (Vertx vertx = Vertx.vertx();)
        and deploy the verticle "RouterClass"

b) RouterClass:
        Only Verticle in the code that registers 2 endpoints ("/" & "/router:id")

To run --> maven build the code and run the .jar (java -jar).
From browser/httpClient --> hit http://localhost:8080/router/<<id>>

================================================================================

How to integrate Prometheus monitoring :

a) MainClass:
    Modify MainClass to create Vertex with Prometheus metric support.
    old code : Vertx vertx = Vertx.vertx();
    New code : Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                               new MicrometerMetricsOptions()
                                       .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
                                       .setEnabled(true)));
b) Router class:
    package : com.sample.vertx.service.verticle
    class   : RouterClass
    code    : add following lines in start() method.
    
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
