/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    public static final String DEFAULT_LOCAL_STRING = "#"; //NON-NLS-1
    public static final String RESOURCE_TYPE_NAME = "name"; //NON-NLS-1
    public static final String RESOURCE_TYPE_DESCRIPTION = "description"; //NON-NLS-1

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

    Class<? extends ILabelProvider> labelProvider() default ILabelProvider.class;

    Class<? extends IPropertyValueTransformer> valueTransformer() default IPropertyValueTransformer.class;

    Class<? extends IPropertyValueEditorProvider> valueEditor() default IPropertyValueEditorProvider.class;

    Class<? extends IPropertyValueListProvider> listProvider() default IPropertyValueListProvider.class;

}
