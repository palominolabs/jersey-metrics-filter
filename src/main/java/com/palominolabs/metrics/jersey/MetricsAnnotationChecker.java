package com.palominolabs.metrics.jersey;

/**
 * Abstraction around checking whether a particular feature is enabled on @ResourceMetrics.
 */
interface MetricsAnnotationChecker {

    /**
     * @param ann annotation to check
     * @return true if the feature relevant to the implementor is enabled
     */
    boolean check(ResourceMetrics ann);
}
