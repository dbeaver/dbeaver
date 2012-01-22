/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.util.List;

/**
 * DBCColumnMetaData
 */
public interface DBCColumnMetaData extends DBSColumnBase
{
    int getIndex();

    String getLabel();

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
    DBSEntityAttribute getTableColumn(DBRProgressMonitor monitor) throws DBException;

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
     * @return list of foreign keys. List can be empty or result can be null if this column is not a reference
     * @throws DBCException on any DB error  @param monitor
     */
    List<DBSEntityReferrer> getReferrers(DBRProgressMonitor monitor) throws DBException;

}
