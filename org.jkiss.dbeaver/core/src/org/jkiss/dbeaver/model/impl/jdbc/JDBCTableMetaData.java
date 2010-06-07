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
import org.jkiss.dbeaver.model.struct.DBSIndex;
import org.jkiss.dbeaver.model.struct.DBSIndexColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

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
            // Check constraints
            for (DBSConstraint constraint : table.getConstraints()) {
                if (constraint.getConstraintType().isUnique()) {
                    // We need ALL columns from this constraint
                    List<JDBCColumnMetaData> rsColumns = new ArrayList<JDBCColumnMetaData>();
                    Collection<? extends DBSConstraintColumn> constrColumns = constraint.getColumns();
                    for (DBSConstraintColumn constrColumn : constrColumns) {
                        JDBCColumnMetaData rsColumn = getColumnMetaData(constrColumn.getTableColumn());
                        if (rsColumn == null) {
                            break;
                        }
                        rsColumns.add(rsColumn);
                    }
                    if (rsColumns.isEmpty() || rsColumns.size() < constrColumns.size()) {
                        // Not all columns are here
                        continue;
                    }
                    identifiers.add(
                        new JDBCTableIdentifier(constraint, rsColumns));
                }
            }
            if (identifiers.isEmpty()) {
                // Check indexes only if no unique constraints found
                for (DBSIndex index : table.getIndexes()) {
                    if (index.isUnique()) {
                        // We need ALL columns from this constraint
                        List<JDBCColumnMetaData> rsColumns = new ArrayList<JDBCColumnMetaData>();
                        Collection<? extends DBSIndexColumn> constrColumns = index.getColumns();
                        for (DBSIndexColumn indexColumn : constrColumns) {
                            JDBCColumnMetaData rsColumn = getColumnMetaData(indexColumn.getTableColumn());
                            if (rsColumn == null) {
                                break;
                            }
                            rsColumns.add(rsColumn);
                        }
                        if (rsColumns.isEmpty() || rsColumns.size() < constrColumns.size()) {
                            // Not all columns are here
                            continue;
                        }
                        identifiers.add(
                            new JDBCTableIdentifier(index, rsColumns));
                    }
                }
            }
        }
        // Find PK or unique key
        DBCTableIdentifier uniqueId = null;
        DBCTableIdentifier uniqueIndex = null;
        for (DBCTableIdentifier id : identifiers) {
            if (id.getConstraint() != null) {
                if (id.getConstraint().getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
                    return id;
                } else if (id.getConstraint().getConstraintType() == DBSConstraintType.UNIQUE_KEY) {
                    uniqueId = id;
                }
            } else {
                uniqueIndex = id;
            }
        }
        return uniqueId != null ? uniqueId : uniqueIndex;
    }

    private JDBCColumnMetaData getColumnMetaData(DBSTableColumn column)
        throws DBException
    {
        for (JDBCColumnMetaData meta : columns) {
            if (meta.getTableColumn() == column) {
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
