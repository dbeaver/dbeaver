/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

/**
 * Property value provider
 */
public interface IPropertyValueListProvider<OBJECT_TYPE> {

    boolean allowCustomValue();

    Object[] getPossibleValues(OBJECT_TYPE object);

}
