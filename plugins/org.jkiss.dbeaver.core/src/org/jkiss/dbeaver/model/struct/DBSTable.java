/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * Table
 */
public interface DBSTable extends DBSEntity, DBPQualifiedObject
{

    boolean isView();

    DBSObjectContainer getContainer();

    /**
     * Table columns
     * @return list of columns
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSTableColumn> getColumns(DBRProgressMonitor monitor) throws DBException;

    /**
     * Retrieve table column by it's name (case insensitive)
     * @param monitor
     *@param columnName column name  @return column or null
     * @throws DBException on any DB error
     */
    DBSTableColumn getColumn(DBRProgressMonitor monitor, String columnName) throws DBException;

    /**
     * Table indices
     * @return list of indices
     * @throws DBException  on any DB error
     * @param monitor
     */
    Collection<? extends DBSIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    /**
     * Keys are: primary keys and unique keys.
     * Foreign keys can be obtained with {@link #getReferences(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor)}
     * @return list of constraints
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets this table foreign keys
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException;

    /**
     * Gets foreign keys which refers this table
     * @return foreign keys list
     * @throws DBException on any DB error
     * @param monitor
     */
    Collection<? extends DBSForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException;

}
