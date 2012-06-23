/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.Collection;

/**
 * Data type provider
 */
public interface DBPDataTypeProvider
{
    /**
     * Retrieves list of supported datatypes
     * @return list of types
     */
    Collection<? extends DBSDataType> getDataTypes();

    /**
     * Gets data type with specified name
     *
     *
     * @param typeName type name
     * @return data type of null
     */
    DBSDataType getDataType(String typeName);

}
