/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Standard filter for property view
 */
public class PropertyViewFilter implements IFilter {

    public static final PropertyViewFilter INSTANCE = new PropertyViewFilter();

    @Override
    public boolean select(Object toTest)
    {
        return toTest instanceof IPropertyDescriptor && !"name".equals(((IPropertyDescriptor)toTest).getId());
    }
}
