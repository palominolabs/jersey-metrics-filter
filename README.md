# What it does

This library uses [Jersey 1](https://jersey.java.net/), [Metrics](http://metrics.codahale.com/) and [Guice](https://code.google.com/p/google-guice/) to simplify gathering performance metrics for your JAX-RS resource methods.

If you have a resource class like this:
```
@Path("whatever")
public class SomeResource {
    @GET
    public String get() {
        return "some data";
    }
}
```

then by using this library you will get a [Timer](http://metrics.codahale.com/manual/core/#timers) metric generated for the `get()` method, as well as [Counters](http://metrics.codahale.com/manual/core/#counters) for each status code returned by `get()`. In this case, `get()` only returns 200, so you would have one counter for status code 200.

# Installation

The first step is to add this library's module and its prerequisites.
- `ResourceMethodMetricsModule` is the module for this library.
- `ResourceMethodWrappedDispatchModule` is needed so that method invocation times can be captured without resorting to thread locals or other such unpleasantness.
- `ConfigModuleBuilder` is used to assemble the config sources for [config-inject](https://github.com/palominolabs/config-inject). If you just want the defaults used, you need not provide any config sources, so you can just call build() as shown. See [JerseyMetricsConfig](https://github.com/palominolabs/jersey-metrics-filter/blob/master/src/main/java/com/palominolabs/metrics/jersey/JerseyMetricsConfig.java) for more.
- We also bind a MetricRegistry instance. The binding uses a binding annotation because it's impolite for a library to insist on an un-qualified binding of a common type like MetricRegistry. This MetricRegistry instance is what will be used to house all metrics generated by the library.

```
// in your Guice module
@Override
protected void configure() {
    install(new ResourceMethodMetricsModule());

    // required for resource method metrics
    install(new ResourceMethodWrappedDispatchModule());
    install(new ConfigModuleBuilder().build());

    MetricRegistry registry = new MetricRegistry();
    bind(MetricRegistry.class).annotatedWith(JerseyResourceMetrics.class).toInstance(registry);

    ...
```

Next, you'll want to make sure you're providing init params to the `GuiceContainer` servlet that provides Jersey/Guice integration so that http status codes can be captured. `HttpStatusCodeCounterResourceFilterFactory` registers a Jersey container response filter that feeds outgoing HTTP status codes to the appropriate counters.
```
// in your Guice module
...
final Map<String, String> initParams = new HashMap<>();
initParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
    HttpStatusCodeCounterResourceFilterFactory.class.getCanonicalName());

install(new ServletModule() {
    @Override
    protected void configureServlets() {
        serve("/*").with(GuiceContainer.class, initParams);
    }
}
...
});
```
# Configuration

At this point, you should now be getting metrics generated for every resource method. If you want to use annotations to have more control, you can use [`@ResourceMetrics`](https://github.com/palominolabs/jersey-metrics-filter/blob/master/src/main/java/com/palominolabs/metrics/jersey/ResourceMetrics.java) to turn both timing and status code counters off and on for a class or method. In this case below, the method would end up having a timer metric but no status code counters.
```
@Path("somewhere")
@ResourceMetrics(statusCodeCounter = true, timer = false)
public class EnabledOnClassDisabledOnMethod {
    // method annotation overrides class annotation
    @GET
    @ResourceMetrics(statusCodeCounter = false, timer = true)
    public String get() {
        return "ok";
    }
}
```

You can also make it so that the default is to *not* create metrics for un-annotated classes and methods by setting the properties used in [JerseyMetricsConfig](https://github.com/palominolabs/jersey-metrics-filter/blob/master/src/main/java/com/palominolabs/metrics/jersey/JerseyMetricsConfig.java). You can do this via the `ConfigModuleBuilder`'s config stack. Here, we'll use a simple in-code Map to define properties.

```
Map<String, Object> config = new HashMap<>();
config.put("com.palominolabs.jersey.metrics.resourceMethod.timer.enabledByDefault", "false");

ConfigModuleBuilder builder = new ConfigModuleBuilder();
// read from a map
builder.addConfiguration(new MapConfiguration(config));
```
