/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCEntityIdentifier {

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
            tableColumns.add(cColumn.getAttribute());
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

    @Override
    public DBSTableConstraint getConstraint()
    {
        return constraint;
    }

    @Override
    public DBSTableIndex getIndex()
    {
        return index;
    }

    @Override
    public List<JDBCColumnMetaData> getResultSetColumns()
    {
        return columns;
    }

    @Override
    public List<? extends DBSTableColumn> getTableColumns()
    {
        return tableColumns;
    }

}