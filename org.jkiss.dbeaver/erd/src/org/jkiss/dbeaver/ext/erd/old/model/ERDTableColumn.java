/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * ERDTableColumn
 */
public class ERDTableColumn implements Comparable<ERDTableColumn> {

    private ERDTable table;
    private DBSTableColumn metaColumn;
    private String name;
    private int keyNumber;

    public ERDTableColumn(DBRProgressMonitor monitor, ERDTable table, DBSTableColumn metaColumn)
        throws DBException
    {
        this.table = table;
        this.metaColumn = metaColumn;
        this.name = metaColumn.getName();
        this.keyNumber = 0;
        // Try to get key number from primary key constraint metaColumn
        DBSConstraint primaryKey = table.getPrimaryKey(monitor);
        if (primaryKey != null) {
            for (DBSConstraintColumn constrCol : primaryKey.getColumns(monitor)) {
                if (constrCol.getTableColumn().equals(metaColumn)) {
                    keyNumber = constrCol.getOrdinalPosition();
                    break;
                }
            }
        }
    }

    public ERDTable getTable()
    {
        return table;
    }

    public DBSTableColumn getMetaColumn()
    {
        return metaColumn;
    }

    public String getName()
    {
        return name;
    }

    public boolean isKey()
    {
        return keyNumber > 0;
    }

    public int getKeyNumber()
    {
        return keyNumber;
    }

    public int compareTo(ERDTableColumn o)
    {
        return
            keyNumber > 0 ? -keyNumber : metaColumn.getOrdinalPosition() -
            o.keyNumber > 0 ? -o.keyNumber : o.metaColumn.getOrdinalPosition();
    }

}
