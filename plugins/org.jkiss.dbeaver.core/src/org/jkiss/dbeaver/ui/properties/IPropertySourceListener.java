/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Property source listener
 */
public interface IPropertySourceListener {

    void handlePropertyChange(Object editableValue, IPropertyDescriptor prop, Object value);


}
