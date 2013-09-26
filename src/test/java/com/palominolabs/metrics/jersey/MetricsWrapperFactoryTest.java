/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.palominolabs.metrics.jersey.MetricsWrapperFactory.EnabledState;
import com.sun.jersey.api.model.AbstractResource;
import com.sun.jersey.api.model.AbstractResourceMethod;
import org.junit.Test;

import javax.ws.rs.Path;
import java.lang.annotation.Annotation;

import static com.palominolabs.metrics.jersey.MetricsWrapperFactory.EnabledState.OFF;
import static com.palominolabs.metrics.jersey.MetricsWrapperFactory.EnabledState.ON;
import static com.palominolabs.metrics.jersey.MetricsWrapperFactory.EnabledState.UNSPECIFIED;
import static com.palominolabs.metrics.jersey.MetricsWrapperFactory.getState;
import static org.junit.Assert.assertEquals;

public final class MetricsWrapperFactoryTest {

    @Test
    public void testGetStateDefault() {
        doMethodStateTest(UNSPECIFIED, FooResource.class, new Annotation[0]);
    }

    @Test
    public void testGetStateMethodEnabled() {
        doMethodStateTest(ON, FooResource.class, new Annotation[]{new ResourceMetricsImpl(true)});
    }

    @Test
    public void testGetStateMethodDisabled() {
        doMethodStateTest(OFF, FooResource.class, new Annotation[]{new ResourceMetricsImpl(false)});
    }

    @Test
    public void testGetStateMethodEnabledClassDisabled() {
        doMethodStateTest(ON, FooResourceDisabled.class, new Annotation[]{new ResourceMetricsImpl(true)});
    }

    @Test
    public void testGetStateMethodDisabledClassEnabled() {
        doMethodStateTest(OFF, FooResourceEnabled.class, new Annotation[]{new ResourceMetricsImpl(false)});
    }

    @Test
    public void testGetStateClassEnabled() {
        doMethodStateTest(ON, FooResourceEnabled.class, new Annotation[]{});
    }

    @Test
    public void testGetStateClassDisabled() {
        doMethodStateTest(OFF, FooResourceDisabled.class, new Annotation[]{});
    }

    private static AbstractResourceMethod getAbstractMethod(Class<?> resourceClass, Annotation[] methodAnnotations) {
        return new AbstractResourceMethod(new AbstractResource(resourceClass), null, Void.class, Void.class, "GET",
            methodAnnotations);
    }

    private static void doMethodStateTest(EnabledState expected, Class<?> resourceClass,
                                          Annotation[] methodAnnotations) {
        assertEquals(expected, getState(getAbstractMethod(resourceClass, methodAnnotations)));
    }

    @Path("/foo")
    private static class FooResource {

    }

    @ResourceMetrics(enabled = false)
    private static class FooResourceDisabled {

    }

    @ResourceMetrics
    private static class FooResourceEnabled {

    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class ResourceMetricsImpl implements ResourceMetrics {

        private final boolean enabled;

        private ResourceMetricsImpl(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ResourceMetrics.class;
        }
    }
}
