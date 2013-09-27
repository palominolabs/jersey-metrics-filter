package com.palominolabs.metrics.jersey;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.ning.http.client.AsyncHttpClient;
import com.palominolabs.config.ConfigModuleBuilder;
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodWrappedDispatchModule;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FullStackTest {

    private static final int PORT = 18080;
    private Server server;

    private AsyncHttpClient httpClient;

    private ConsoleReporter consoleReporter;

    private MetricRegistry metricRegistry;

    @Before
    public void setUp() throws Exception {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();

        final MetricRegistry registry = new MetricRegistry();
        metricRegistry = registry;

        final Map<String, String> initParams = new HashMap<>();
        initParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            HttpStatusCodeCounterResourceFilterFactory.class.getCanonicalName());
        initParams.put(ResourceConfig.FEATURE_DISABLE_WADL, "true");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                binder().requireExplicitBindings();
                install(new ResourceMethodWrappedDispatchModule());
                install(new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve("/*").with(GuiceContainer.class, initParams);
                    }
                });
                install(new JerseyServletModule());
                bind(GuiceFilter.class);
                bind(GuiceContainer.class);
                bind(EnabledOnClass.class);
                bind(DisabledOnClass.class);
                bind(EnabledOnClassDisabledOnMethod.class);

                install(new ConfigModuleBuilder().build());
                install(new ResourceMethodMetricsModule());
                bind(MetricRegistry.class).annotatedWith(JerseyResourceMetrics.class).toInstance(registry);
            }
        });

        httpClient = new AsyncHttpClient();

        server = getServer(injector.getInstance(GuiceFilter.class));
        server.start();

        consoleReporter = ConsoleReporter.forRegistry(registry).build();
    }

    @After
    public void tearDown() throws Exception {
        consoleReporter.report();

        consoleReporter.stop();
        server.stop();
    }

    @Test
    public void testFullStack() throws IOException, ExecutionException, InterruptedException,
        MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException,
        InstanceNotFoundException {
        assertEquals(200,
            httpClient.prepareGet("http://localhost:" + PORT + "/enabledOnClass").execute().get().getStatusCode());

        // these other two resource classes should not have metrics
        assertEquals(200,
            httpClient.prepareGet("http://localhost:" + PORT + "/disabledOnClass").execute().get().getStatusCode());

        assertEquals(200,
            httpClient.prepareGet("http://localhost:" + PORT + "/enabledOnClassDisabledOnMethod").execute().get()
                .getStatusCode());

        SortedMap<String, Timer> timers = metricRegistry.getTimers();

        // check names
        Set<String> timerNames = Sets.newHashSet(

            "com.palominolabs.metrics.jersey.FullStackTest$EnabledOnClass./enabledOnClass GET timer");

        assertEquals(timerNames, timers.keySet());

        SortedMap<String, Counter> counters = metricRegistry.getCounters();

        Set<String> counterNames = Sets.newHashSet(
            "com.palominolabs.metrics.jersey.FullStackTest$EnabledOnClass./enabledOnClass GET 200 counter");

        assertEquals(counterNames, counters.keySet());

        // check values

        Timer timer = timers.get(timerNames.iterator().next());
        assertEquals(1, timer.getCount());
        assertTrue(timer.getMeanRate() > 0D);

        Counter counter = counters.get(counterNames.iterator().next());
        assertEquals(1, counter.getCount());
    }

    private Server getServer(GuiceFilter filter) {
        Server server = new Server(PORT);
        ServletContextHandler servletHandler = new ServletContextHandler();

        servletHandler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
                IOException {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("text/plain");
                resp.setContentType("UTF-8");
                resp.getWriter().append("404");
            }
        }), "/*");

        // add guice servlet filter
        servletHandler.addFilter(new FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType.class));

        server.setHandler(servletHandler);

        return server;
    }

    @Path("enabledOnClass")
    @ResourceMetrics
    public static class EnabledOnClass {
        @GET
        public String get() {
            return "ok";
        }
    }

    @Path("disabledOnClass")
    @ResourceMetrics(statusCodeCounter = false, timer = false)
    public static class DisabledOnClass {
        @GET
        public String get() {
            return "ok";
        }
    }

    @Path("enabledOnClassDisabledOnMethod")
    @ResourceMetrics
    public static class EnabledOnClassDisabledOnMethod {
        @GET
        @ResourceMetrics(statusCodeCounter = false, timer = false)
        public String get() {
            return "ok";
        }
    }
}
