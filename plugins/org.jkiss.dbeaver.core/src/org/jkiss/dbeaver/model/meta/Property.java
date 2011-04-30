/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.eclipse.jface.viewers.ILabelProvider;

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

    boolean expensive() default false;

    int order() default Integer.MAX_VALUE;

    String helpContextId() default "";

    Class<ILabelProvider> labelProvider() default ILabelProvider.class;

    Class<? extends IPropertyValueEditor> valueEditor() default IPropertyValueEditor.class;

    Class<? extends IPropertyValueListProvider> listProvider() default IPropertyValueListProvider.class;

}
