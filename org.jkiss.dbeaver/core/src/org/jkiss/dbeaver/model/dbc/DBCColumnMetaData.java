/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * DBCColumnMetaData
 */
public interface DBCColumnMetaData extends DBSTypedObject
{
    int getIndex();

    boolean isAutoIncrement();

    boolean isNullable();

    int getDisplaySize();

    String getLabel();

    String getColumnName();

    int getPrecision();

    int getScale();

    String getTableName();

    String getCatalogName();

    String getSchemaName();

    boolean isReadOnly();

    boolean isWritable();


    /**
     * Owner table metadata
     * @return table metadata
     * @throws DBCException on any DB error
     */
    DBSTableColumn getTableColumn() throws DBException;

    /**
     * Owner table metadata
     * @return table metadata
     * @throws DBCException on any DB error
     */
    DBCTableMetaData getTable() throws DBException;

    /**
     * Check this column is a reference.
     * Reference columns are included in one or more foreign keys. 
     * @return true or false.
     * @throws DBCException on any DB error
     */
    boolean isReference() throws DBException;

    /**
     * Gets list of foreign keys in which this column is contained.
     * @return list of foreign keys. List can be empty or result can be null if this column is not a reference
     * @throws DBCException on any DB error
     */
    List<DBSForeignKey> getReferences() throws DBException;

}
