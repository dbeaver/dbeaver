/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;

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
    private JDBCResultSet resultSet;
    private ResultSetMetaData jdbcMetaData;
    private List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
    private Map<String, JDBCTableMetaData> tables = new HashMap<String, JDBCTableMetaData>();

    JDBCResultSetMetaData(JDBCResultSet resultSet)
        throws SQLException
    {
        this.resultSet = resultSet;
        this.jdbcMetaData = resultSet.getResultSet().getMetaData();
        int count = jdbcMetaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            columns.add(new JDBCColumnMetaData(this, i));
        }
    }

    public JDBCResultSet getResultSet()
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
        DBSObject catalog = schema == null ? null : schema.getParentObject();

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
