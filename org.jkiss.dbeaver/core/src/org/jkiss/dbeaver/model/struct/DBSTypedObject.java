/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSColumnDefinition
 */
public interface DBSTypedObject
{
    /**
     * Database specific type name
     * @return type name
     */
    String getTypeName();

    /**
     * JDBC type number.
     * Refer java.sql.Types for possible values
     * @return value type
     */
    int getValueType();

}