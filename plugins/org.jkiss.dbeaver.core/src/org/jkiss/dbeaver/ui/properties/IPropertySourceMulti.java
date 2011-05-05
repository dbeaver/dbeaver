/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * Property source which allows editing of multiple objects.
 */
public interface IPropertySourceMulti extends IPropertySource2 {

    boolean isPropertySet(Object object, ObjectPropertyDescriptor id);

    Object getPropertyValue(Object object, ObjectPropertyDescriptor prop);

    boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop);

    void resetPropertyValue(Object object, ObjectPropertyDescriptor prop);

    void setPropertyValue(Object object, ObjectPropertyDescriptor prop, Object value);

}
