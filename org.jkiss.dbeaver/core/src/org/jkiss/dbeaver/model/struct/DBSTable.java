/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;

import java.util.Collection;

/**
 * Table
 */
public interface DBSTable extends DBSStructureObject
{
    String getFullQualifiedName();

    String getTableType();

    boolean isView();

    DBSStructureContainer getContainer();

    /**
     * Table columns
     * @return list of columns
     * @throws DBException on any DB error
     */
    Collection<? extends DBSTableColumn> getColumns() throws DBException;

    /**
     * Retrieve table column by it's name (case insensitive)
     * @param columnName column name
     * @return column or null
     * @throws DBException on any DB error
     */
    DBSTableColumn getColumn(String columnName) throws DBException;

    /**
     * Table indices
     * @return list of indices
     * @throws DBException  on any DB error
     */
    Collection<? extends DBSIndex> getIndexes() throws DBException;

    /**
     * Constraints are: primary keys and unique keys.
     * Foreign keys can be obtained with {@link #getExportedKeys()}
     * @return list of constraints
     * @throws DBException on any DB error
     */
    Collection<? extends DBSConstraint> getConstraints() throws DBException;

    /**
     * Gets this table foreign keys
     * @return foreign keys list
     * @throws DBException on any DB error
     */
    Collection<? extends DBSForeignKey> getExportedKeys() throws DBException;

    /**
     * Gets this foreign keys which refers this table
     * @return foreign keys list
     * @throws DBException on any DB error
     */
    Collection<? extends DBSForeignKey> getImportedKeys() throws DBException;

}
