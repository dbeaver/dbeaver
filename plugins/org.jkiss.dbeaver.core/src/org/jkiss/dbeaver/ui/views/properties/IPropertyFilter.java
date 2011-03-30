package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * Property filter
 */
public interface IPropertyFilter {

    boolean isValid(IPropertyDescriptor property);

}
