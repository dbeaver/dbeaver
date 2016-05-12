/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    String DEFAULT_LOCAL_STRING = "#"; //NON-NLS-1
    String RESOURCE_TYPE_NAME = "name"; //NON-NLS-1
    String RESOURCE_TYPE_DESCRIPTION = "description"; //NON-NLS-1

    /**
     * Property unique ID (unique within class)
     * @return id
     */
    String id() default ""; //NON-NLS-1

    /**
     * Property human readable name
     * @return name
     */
    String name() default DEFAULT_LOCAL_STRING;

    /**
     * Property category (optional). A human readable string
     * @return category
     */
    String category() default ""; //NON-NLS-1

    /**
     * Property description (optional)
     * @return description
     */
    String description() default DEFAULT_LOCAL_STRING;

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

    String helpContextId() default ""; //NON-NLS-1

    /**
     * Can be used to format numbers and date/time property values
     */
    String format() default ""; //NON-NLS-1

    Class<? extends IPropertyValueTransformer> valueTransformer() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueListProvider> listProvider() default IPropertyValueListProvider.class;

    Class<? extends IPropertyValueTransformer> valueRenderer() default IPropertyValueTransformer.class;

}
