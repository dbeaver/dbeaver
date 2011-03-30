package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Standard filter for property view
 */
public class PropertyViewFilter implements IPropertyFilter {

    public static final PropertyViewFilter INSTANCE = new PropertyViewFilter();

    public boolean isValid(IPropertyDescriptor property)
    {
        return !"name".equals(property.getId());
    }
}
