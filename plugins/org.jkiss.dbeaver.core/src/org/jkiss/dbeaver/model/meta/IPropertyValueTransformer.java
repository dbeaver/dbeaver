/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

/**
 * Value transformer
 */
public interface IPropertyValueTransformer<OBJECT_TYPE, PROP_TYPE> {

    PROP_TYPE transform(OBJECT_TYPE object, PROP_TYPE value);

}
