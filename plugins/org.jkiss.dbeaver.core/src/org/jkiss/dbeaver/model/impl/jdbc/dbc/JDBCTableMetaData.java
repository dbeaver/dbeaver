/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCEntityMetaData {

    private JDBCResultSetMetaData resultSetMetaData;
    private String catalogName;
    private String schemaName;
    private String tableName;
    private String alias;
    private List<JDBCColumnMetaData> columns = new ArrayList<JDBCColumnMetaData>();
    private List<JDBCTableIdentifier> identifiers;
    private DBSTable table;

    public JDBCTableMetaData(JDBCResultSetMetaData resultSetMetaData, DBSTable table, String catalogName, String schemaName, String tableName, String alias)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.table = table;
        this.alias = alias;
    }

    public JDBCResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    @Override
    public DBSTable getEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        if (table == null) {
            final DBPDataSource dataSource = resultSetMetaData.getResultSet().getContext().getDataSource();
            final DBSObjectContainer sc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            if (sc != null) {
                Class<? extends DBSObject> scChildType = sc.getChildType(monitor);
                DBSObject tableObject;
                if (!CommonUtils.isEmpty(catalogName) && scChildType != null && DBSSchema.class.isAssignableFrom(scChildType)) {
                    // Do not use catalog name
                    // Some data sources do not load catalog list but result set meta data contains one (e.g. DB2)
                    tableObject = DBUtils.getObjectByPath(monitor, sc, null, schemaName, tableName);
                } else {
                    tableObject = DBUtils.getObjectByPath(monitor, sc, catalogName, schemaName, tableName);
                }
                if (tableObject == null) {
                    throw new DBException("Table '" + tableName + "' not found in metadata catalog");
                } else if (tableObject instanceof DBSTable) {
                    table = (DBSTable) tableObject;
                } else {
                    throw new DBException("Unsupported table class: " + tableObject.getClass().getName());
                }
            }
        }
        return table;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    @Override
    public String getEntityName()
    {
        return tableName;
    }

    @Override
    public String getEntityAlias()
    {
        return alias;
    }

    @Override
    public boolean isIdentified(DBRProgressMonitor monitor)
        throws DBException
    {
        return getBestIdentifier(monitor) != null;
    }

    @Override
    public DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor)
        throws DBException
    {
        DBSTable table = getEntity(monitor);

        if (table.isView()) {
            return null;
        }
        if (identifiers == null) {
            // Load identifiers
            identifiers = new ArrayList<JDBCTableIdentifier>();
            // Check constraints
            Collection<? extends DBSTableConstraint> uniqueKeys = table.getConstraints(monitor);
            if (!CommonUtils.isEmpty(uniqueKeys)) {
                for (DBSTableConstraint constraint : uniqueKeys) {
                    if (constraint.getConstraintType().isUnique()) {
                        // We need ALL columns from this constraint
                        Collection<? extends DBSTableConstraintColumn> constrColumns = constraint.getColumns(monitor);
                        if (!CommonUtils.isEmpty(constrColumns)) {
                            List<JDBCColumnMetaData> rsColumns = new ArrayList<JDBCColumnMetaData>();
                            for (DBSTableConstraintColumn constrColumn : constrColumns) {
                                JDBCColumnMetaData rsColumn = getColumnMetaData(monitor, constrColumn.getAttribute());
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
                                new JDBCTableIdentifier(monitor, constraint, rsColumns));
                        }
                    }
                }
            }
            if (identifiers.isEmpty()) {
                // Check indexes only if no unique constraints found
                Collection<? extends DBSTableIndex> indexes = table.getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSTableIndex index : indexes) {
                        if (index.isUnique()) {
                            // We need ALL columns from this constraint
                            Collection<? extends DBSTableIndexColumn> constrColumns = index.getColumns(monitor);
                            if (!CommonUtils.isEmpty(constrColumns)) {
                                List<JDBCColumnMetaData> rsColumns = new ArrayList<JDBCColumnMetaData>();
                                for (DBSTableIndexColumn indexColumn : constrColumns) {
                                    JDBCColumnMetaData rsColumn = getColumnMetaData(monitor, indexColumn.getTableColumn());
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
                                    new JDBCTableIdentifier(monitor, index, rsColumns));
                            }
                        }
                    }
                }
            }
        }
        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBCEntityIdentifier uniqueId = null;
            DBCEntityIdentifier uniqueIndex = null;
            for (DBCEntityIdentifier id : identifiers) {
                if (id.getConstraint() != null) {
                    if (id.getConstraint().getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        return id;
                    } else if (id.getConstraint().getConstraintType().isUnique()) {
                        uniqueId = id;
                    }
                } else {
                    uniqueIndex = id;
                }
            }
            return uniqueId != null ? uniqueId : uniqueIndex;
        } else {
            return null;
        }
    }

    private JDBCColumnMetaData getColumnMetaData(DBRProgressMonitor monitor, DBSTableColumn column)
        throws DBException
    {
        for (JDBCColumnMetaData meta : columns) {
            if (meta.getTableColumn(monitor) == column) {
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
