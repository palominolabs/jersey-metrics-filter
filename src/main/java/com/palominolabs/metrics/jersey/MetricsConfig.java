/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import org.skife.config.Config;
import org.skife.config.Default;

public interface MetricsConfig {

    /**
     * @return true if resource methods should have timing metrics captured by default
     */
    @Config("palominolabs.jersey.metrics.resourceMethod.timer.enabledByDefault")
    @Default("true")
    boolean isTimingEnabledByDefault();

    /**
     * @return true if resource methods should have status code metrics captured by default
     */
    @Config("palominolabs.jersey.metrics.resourceMethod.statusCode.enabledByDefault")
    @Default("true")
    boolean isStatusCodeEnabledByDefault();
}
