/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * Extended property descriptor
 */
public interface IPropertyDescriptorEx extends IPropertyDescriptor {

    Class<?> getDataType();

    boolean isRequired();

    Object getDefaultValue();

}
