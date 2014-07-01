/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import com.sun.jersey.api.model.AbstractResourceMethod;

import javax.annotation.Nonnull;

public interface ResourceMetricNamer {

    /**
     * @param am resource method
     * @return a string name used as the prefix for all metrics about that resource method
     */
    @Nonnull
    String getMetricBaseName(AbstractResourceMethod am);
}
