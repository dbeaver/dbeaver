/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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

    private DBSConstraint constraint;
    private DBSIndex index;
    private List<JDBCColumnMetaData> columns;
    private List<DBSTableColumn> tableColumns;

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSConstraint constraint, List<JDBCColumnMetaData> columns)
    {
        this.constraint = constraint;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSConstraintColumn cColumn : constraint.getColumns(monitor)) {
            tableColumns.add(cColumn.getTableColumn());
        }
    }

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSIndex index, List<JDBCColumnMetaData> columns)
    {
        this.index = index;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSIndexColumn cColumn : index.getColumns(monitor)) {
            tableColumns.add(cColumn.getTableColumn());
        }
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

    public DBSIndex getIndex()
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