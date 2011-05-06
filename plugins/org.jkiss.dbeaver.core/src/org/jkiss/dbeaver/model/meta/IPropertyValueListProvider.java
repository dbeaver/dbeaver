/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

/**
 * Property value provider
 */
public interface IPropertyValueListProvider {

    Object[] getPossibleValues(Object object);

}
