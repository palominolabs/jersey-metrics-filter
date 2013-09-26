/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.metrics.jersey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ResourceMetrics {

    /**
     * @return true if timing should be measured for the annotated method (or all methods on the annotated class)
     */
    boolean timing() default true;

    /**
     * @return true if status codes be measured for the annotated method (or all methods on the annotated class)
     */
    boolean statusCodes() default true;
}
