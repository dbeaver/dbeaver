/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.exec.DBCTableMetaData;

import java.util.Collection;

/**
 * DBD Row Controller
 */
public interface DBDRowController
{
    /**
     * Column meta data
     * @return meta data
     */
    Collection<DBCColumnMetaData> getColumnsMetaData();

    /**
     * Find column metadata by specified table and column name
     * @param table table
     * @param columnName column name
     * @return column meta data or null
     */
    DBCColumnMetaData getColumnMetaData(DBCTableMetaData table, String columnName);

    /**
     * Tries to read value of certain column from result set.
     * @param column column, must belong to the same result set as controller's value
     * @return value or null
     */
    Object getColumnValue(DBCColumnMetaData column);

}