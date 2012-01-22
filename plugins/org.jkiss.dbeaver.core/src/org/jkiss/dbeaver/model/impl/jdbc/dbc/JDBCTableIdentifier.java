/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.dbeaver.model.exec.DBCTableIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCTableIdentifier {

    private DBSTableConstraint constraint;
    private DBSTableIndex index;
    private List<JDBCColumnMetaData> columns;
    private List<DBSTableColumn> tableColumns;

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSTableConstraint constraint, List<JDBCColumnMetaData> columns)
    {
        this.constraint = constraint;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSTableConstraintColumn cColumn : constraint.getColumns(monitor)) {
            tableColumns.add(cColumn.getTableColumn());
        }
    }

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSTableIndex index, List<JDBCColumnMetaData> columns)
    {
        this.index = index;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSTableIndexColumn cColumn : index.getColumns(monitor)) {
            tableColumns.add(cColumn.getTableColumn());
        }
    }

    public DBSTableConstraint getConstraint()
    {
        return constraint;
    }

    public DBSTableIndex getIndex()
    {
        return index;
    }

    public List<JDBCColumnMetaData> getResultSetColumns()
    {
        return columns;
    }

    public List<? extends DBSTableColumn> getTableColumns()
    {
        return tableColumns;
    }

}