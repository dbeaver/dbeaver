/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.eclipse.jface.viewers.ILabelProvider;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

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
    /**
     * Property unique ID (unique within class)
     * @return id
     */
    String id() default "";

    /**
     * Property human readable name
     * @return name
     */
    String name();

    /**
     * Property category (optional). A human readable string
     * @return category
     */
    String category() default "";

    /**
     * Property description (optional)
     * @return description
     */
    String description() default "";

    /**
     * Editable flag. If set to true then property may be edited for new objects.
     * @return editable flag
     */
    boolean editable() default false;

    /**
     * Updatable flag. If set to true then property can be changed on any object
     * @return updatable flag
     */
    boolean updatable() default false;

    /**
     * Viewable flag. Viewable properties are displayed in lists.
     * Note that property editor contains all properties (except hidden).
     * @return viewable flag
     */
    boolean viewable() default false;

    boolean hidden() default false;

    boolean expensive() default false;

    /**
     * Makes sense only for lazy properties. If set to true then this property value can be read
     * in non-lazy way with null progress monitor. In this case it will return "preview" value.
     * @return preview support flag
     */
    boolean supportsPreview() default false;

    int order() default Integer.MAX_VALUE;

    String helpContextId() default "";

    Class<? extends ILabelProvider> labelProvider() default ILabelProvider.class;

    Class<? extends IPropertyValueTransformer> valueTransformer() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueEditorProvider> valueEditor() default IPropertyValueEditorProvider.class;

    Class<? extends IPropertyValueListProvider> listProvider() default IPropertyValueListProvider.class;

}
