/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.api.ResultSetManagable;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBCResultSetMetaData
 */
public class JDBCResultSetMetaData implements DBCResultSetMetaData
{
    private ResultSetManagable resultSet;
    private ResultSetMetaData jdbcMetaData;
    private List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
    private Map<String, JDBCTableMetaData> tables = new HashMap<String, JDBCTableMetaData>();

    public JDBCResultSetMetaData(ResultSetManagable resultSet)
        throws DBCException
    {
        this.resultSet = resultSet;
        try {
            this.jdbcMetaData = resultSet.getMetaData();
            int count = jdbcMetaData.getColumnCount();
            for (int i = 1; i <= count; i++) {
                columns.add(new JDBCColumnMetaData(this, i));
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public DBCResultSet getResultSet()
    {
        return resultSet;
    }

    ResultSetMetaData getJdbcMetaData()
    {
        return jdbcMetaData;
    }

    public List<DBCColumnMetaData> getColumns()
    {
        return columns;
    }

    public JDBCTableMetaData getTableMetaData(String catalogName, String schemaName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(tableName)) {
            // some constant instead of table name
            return null;
        }
        StringBuilder fullName = new StringBuilder();
        if (catalogName != null) fullName.append(catalogName).append("|");
        if (schemaName != null) fullName.append(schemaName).append("|");
        fullName.append(tableName);
        String fullQualifiedName = fullName.toString();

        JDBCTableMetaData tableMetaData = tables.get(fullQualifiedName);
        if (tableMetaData == null) {
            tableMetaData = new JDBCTableMetaData(this, null, catalogName, schemaName, tableName, null);
            tables.put(fullQualifiedName, tableMetaData);
        }
        return tableMetaData;
    }

    public JDBCTableMetaData getTableMetaData(DBSTable table)
        throws DBException
    {
        DBSObject schema = table.getParentObject();
        if (schema instanceof DBSDataSourceContainer) {
            // It's not a schema
            schema = null;
        }
        DBSObject catalog = schema == null ? null : schema.getParentObject();
        if (catalog instanceof DBSDataSourceContainer) {
            // It's not a catalog
            catalog = null;
        }

        StringBuilder fullName = new StringBuilder();
        if (catalog != null) fullName.append(catalog.getName()).append("|");
        if (schema != null) fullName.append(schema.getName()).append("|");
        fullName.append(table.getName());
        String fullQualifiedName = fullName.toString();

        JDBCTableMetaData tableMetaData = tables.get(fullQualifiedName);
        if (tableMetaData == null) {
            tableMetaData = new JDBCTableMetaData(
                this,
                table,
                catalog == null ? null : catalog.getName(),
                schema == null ? null : schema.getName(),
                table.getName(),
                null);
            tables.put(fullQualifiedName, tableMetaData);
        }
        return tableMetaData;
    }
}
