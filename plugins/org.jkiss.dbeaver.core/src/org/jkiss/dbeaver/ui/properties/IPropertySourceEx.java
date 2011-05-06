/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * Property source which supports default property values
 */
public interface IPropertySourceEx extends IPropertySource2 {

    boolean isDirty(Object id);

    boolean hasDefaultValue(Object id);

    void resetPropertyValueToDefault(Object id);
}
