/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodDispatchWrapper;
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodDispatchWrapperChain;
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodDispatchWrapperFactory;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * Factory for dispatch wrappers that wrap request invocation to get timing info.
 */
final class MetricsWrapperFactory implements ResourceMethodDispatchWrapperFactory {

    private final MetricsConfig metricsConfig;

    private final ResourceMetricNamer namer;
    private final MetricsRegistry metricsRegistry;

    @Inject
    MetricsWrapperFactory(MetricsConfig metricsConfig, ResourceMetricNamer namer, MetricsRegistry metricsRegistry) {
        this.metricsConfig = metricsConfig;
        this.namer = namer;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public ResourceMethodDispatchWrapper createDispatchWrapper(AbstractResourceMethod am) {
        EnabledState state = getState(am);

        if (state == EnabledState.OFF || (state == EnabledState.UNSPECIFIED && !metricsConfig.isTimingEnabledByDefault())) {
            return null;
        }

        Class<?> resourceClass = am.getResource().getResourceClass();
        String metricId = namer.getMetricBaseName(am);
        final Timer timer = metricsRegistry.newTimer(resourceClass, metricId + " timer");
        return new ResourceMethodDispatchWrapper() {
            @Override
            public void wrapDispatch(Object resource, HttpContext context, ResourceMethodDispatchWrapperChain chain) {

                final TimerContext time = timer.time();
                try {
                    chain.wrapDispatch(resource, context);
                } finally {
                    time.stop();
                }
            }
        };
    }

    @VisibleForTesting
    static EnabledState getState(AbstractResourceMethod am) {
        // check method, then class
        for (ResourceMetrics ann : new ResourceMetrics[]{am.getAnnotation(ResourceMetrics.class), am
            .getResource().getAnnotation(ResourceMetrics.class)}) {

            if (ann != null) {
                if (ann.enabled()) {
                    return EnabledState.ON;
                } else {
                    return EnabledState.OFF;
                }
            }
        }

        return EnabledState.UNSPECIFIED;
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    @VisibleForTesting
    static enum EnabledState {
        ON,
        OFF,
        UNSPECIFIED
    }
}
