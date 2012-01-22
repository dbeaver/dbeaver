/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

/**
 * Result set table metadata
 */
public interface DBCTableMetaData {

    /**
     * Table reference
     * @return table table reference. never returns null
     * @param monitor progress monitor
     */
    DBSEntity getTable(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Table name
     * @return table name
     */
    String getTableName();

    /**
     * Table alias
     * @return table alias in query
     */
    String getTableAlias();

    /**
     * Checks table is identified.
     * Table is identified if resultset contains at least one set of this table columns which will unique
     * identify table row
     * @return true if this table has at least one unique identifier in the whole resultset.
     * @param monitor progress monitor
     */
    boolean isIdentified(DBRProgressMonitor monitor)
        throws DBException;

    /**
     * Gets best table identifier.
     * Best identifier is a primary key. If no such one then any unique key fits.
     * @return list of identifier columns which identifies this table row the best way
     * or null if no identifiers found.
     * @param monitor progress monitor
     */
    DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor)
        throws DBException;

}
