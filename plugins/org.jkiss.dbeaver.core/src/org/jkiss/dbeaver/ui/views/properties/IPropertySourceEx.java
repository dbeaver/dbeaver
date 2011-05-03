/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Extended property source
 */
public interface IPropertySourceEx extends IPropertySource {

    Object getPropertyValue(Object object, ObjectPropertyDescriptor prop);

    void setPropertyValue(Object object, ObjectPropertyDescriptor prop, Object value);

}
