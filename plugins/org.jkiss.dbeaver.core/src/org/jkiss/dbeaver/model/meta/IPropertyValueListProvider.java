/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import java.util.Collection;

/**
 * Property value provider
 */
public interface IPropertyValueListProvider {

    String[] getPossibleValues(Object object);

}
