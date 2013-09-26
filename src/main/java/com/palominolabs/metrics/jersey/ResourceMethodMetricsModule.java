/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.google.inject.AbstractModule;
import com.palominolabs.config.ConfigModule;
import com.palominolabs.jersey.dispatchwrapper.ResourceMethodWrappedDispatchModule;

public final class ResourceMethodMetricsModule extends AbstractModule{
    @Override
    protected void configure() {
        ConfigModule.bindConfigBean(binder(), MetricsConfig.class);
        bind(ResourceMetricNamer.class).to(ResourceMetricNamerImpl.class);

        ResourceMethodWrappedDispatchModule.bindWrapperFactory(binder(), MetricsWrapperFactory.class);
        bind(HttpStatusCodeMetricResourceFilterFactory.class);
    }
}
