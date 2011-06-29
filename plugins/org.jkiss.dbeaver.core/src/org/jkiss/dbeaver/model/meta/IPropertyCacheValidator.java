package org.jkiss.dbeaver.model.meta;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Property cache validator
 */
public interface IPropertyCacheValidator<OBJECT_TYPE> {

    boolean isPropertyCached(OBJECT_TYPE object, Object propertyId);

}
