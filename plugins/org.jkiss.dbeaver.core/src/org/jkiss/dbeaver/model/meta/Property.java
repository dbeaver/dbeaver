/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Property
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property
{

    String id() default "";

    String name();

    String category() default "";

    String description() default "";

    boolean editable() default false;

    boolean updatable() default false;

    boolean viewable() default false;

    int order() default Integer.MAX_VALUE;
}
