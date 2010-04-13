/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCTableIdentifier;
import org.jkiss.dbeaver.model.dbc.DBCTableMetaData;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCTableMetaData {

    private JDBCResultSetMetaData resultSetMetaData;
    private DBSTable table;
    private String alias;
    private List<JDBCColumnMetaData> columns = new ArrayList<JDBCColumnMetaData>();
    private List<JDBCTableIdentifier> identifiers;

    public JDBCTableMetaData(JDBCResultSetMetaData resultSetMetaData, DBSTable table, String alias)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.table = table;
        this.alias = alias;
    }

    public JDBCResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    public DBSTable getTable()
    {
        return table;
    }

    public String getTableName()
    {
        return table.getName();
    }

    public String getTableAlias()
    {
        return alias;
    }

    public boolean isIdentitied()
        throws DBException
    {
        return getBestIdentifier() != null;
    }

    public DBCTableIdentifier getBestIdentifier()
        throws DBException
    {
        if (identifiers == null) {
            // Load identifiers
            identifiers = new ArrayList<JDBCTableIdentifier>();

            Collection<? extends DBSConstraint> constraints = table.getConstraints();
            for (DBSConstraint constraint : constraints) {
                if (constraint.getConstraintType().isUnique()) {
                    // We need ALL columns from this constraint
                    List<JDBCColumnMetaData> rsColumns = new ArrayList<JDBCColumnMetaData>();
                    Collection<? extends DBSConstraintColumn> constrColumns = constraint.getColumns();
                    for (DBSConstraintColumn constrColumn : constrColumns) {
                        JDBCColumnMetaData rsColumn = getColumnMetaData(constrColumn);
                        if (rsColumn == null) {
                            break;
                        }
                        rsColumns.add(rsColumn);
                    }
                    if (rsColumns.size() < constrColumns.size()) {
                        // Not all columns are here
                        continue;
                    }
                    identifiers.add(
                        new JDBCTableIdentifier(constraint, rsColumns));
                }
            }
        }
        // Find PK or unique key
        DBCTableIdentifier uniqueId = null;
        for (DBCTableIdentifier id : identifiers) {
            if (id.getConstraint().getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
                return id;
            } else if (id.getConstraint().getConstraintType() == DBSConstraintType.UNIQUE_KEY) {
                uniqueId = id;
            }
        }
        return uniqueId;
    }

    private JDBCColumnMetaData getColumnMetaData(DBSConstraintColumn constrColumn)
        throws DBException
    {
        for (JDBCColumnMetaData meta : columns) {
            if (meta.getTableColumn() == constrColumn.getTableColumn()) {
                return meta;
            }
        }
        return null;
    }

    public List<JDBCColumnMetaData> getColumns()
    {
        return columns;
    }

    void addColumn(JDBCColumnMetaData columnMetaData)
    {
        columns.add(columnMetaData);
    }

}
