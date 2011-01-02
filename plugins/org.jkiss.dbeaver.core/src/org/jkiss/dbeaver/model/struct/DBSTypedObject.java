/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * DBSTypedObject
 */
public interface DBSTypedObject extends DBPObject
{
    /**
     * Database specific type name
     * @return type name
     */
    String getTypeName();

    /**
     * Type number.
     * Refer java.sql.Types for possible values
     * @return value type
     */
    int getValueType();

    /**
     * Value scale
     * @return scale
     */
    int getScale();

    /**
     * Value precision
     * @return precision
     */
    int getPrecision();

}