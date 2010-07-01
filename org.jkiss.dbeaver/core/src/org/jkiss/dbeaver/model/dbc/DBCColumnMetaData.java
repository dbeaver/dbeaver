/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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

    String getTableName();

    String getCatalogName();

    String getSchemaName();

    boolean isReadOnly();

    boolean isWritable();


    /**
     * Column metadata
     * @return column metadata
     * @throws DBCException on any DB error
     * @param monitor
     */
    DBSTableColumn getTableColumn(DBRProgressMonitor monitor) throws DBException;

    /**
     * Owner table metadata
     * @return table metadata
     * @throws DBCException on any DB error
     */
    DBCTableMetaData getTable();

    /**
     * Check this column is a foreign key.
     * Reference columns are included in one or more foreign keys. 
     * @return true or false.
     * @throws DBCException on any DB error
     * @param monitor
     */
    boolean isForeignKey(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets list of foreign keys in which this column is contained.
     * @param unique if true then returns references in which this column is the only key
     * @return list of foreign keys. List can be empty or result can be null if this column is not a reference
     * @throws DBCException on any DB error  @param monitor
     * @param unique
     */
    List<DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor, boolean unique) throws DBException;

}
