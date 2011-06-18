/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.api.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
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
public class JDBCResultSetMetaData implements DBCResultSetMetaData, ResultSetMetaData
{
    private JDBCResultSetImpl resultSet;
    private ResultSetMetaData jdbcMetaData;
    private List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
    private Map<String, JDBCTableMetaData> tables = new HashMap<String, JDBCTableMetaData>();

    public JDBCResultSetMetaData(JDBCResultSetImpl resultSet)
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

    public JDBCResultSetMetaData(JDBCResultSetImpl resultSet, ResultSetMetaData original)
    {
        this.resultSet = resultSet;
        this.jdbcMetaData = original;
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

    public int getColumnCount()
        throws SQLException
    {
        return jdbcMetaData.getColumnCount();
    }

    public boolean isAutoIncrement(int column)
        throws SQLException
    {
        return jdbcMetaData.isAutoIncrement(column);
    }

    public boolean isCaseSensitive(int column)
        throws SQLException
    {
        return jdbcMetaData.isCaseSensitive(column);
    }

    public boolean isSearchable(int column)
        throws SQLException
    {
        return jdbcMetaData.isSearchable(column);
    }

    public boolean isCurrency(int column)
        throws SQLException
    {
        return jdbcMetaData.isCurrency(column);
    }

    public int isNullable(int column)
        throws SQLException
    {
        return jdbcMetaData.isNullable(column);
    }

    public boolean isSigned(int column)
        throws SQLException
    {
        return jdbcMetaData.isSigned(column);
    }

    public int getColumnDisplaySize(int column)
        throws SQLException
    {
        return jdbcMetaData.getColumnDisplaySize(column);
    }

    public String getColumnLabel(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnLabel(column));
    }

    public String getColumnName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnName(column));
    }

    public String getSchemaName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getSchemaName(column));
    }

    public int getPrecision(int column)
        throws SQLException
    {
        return jdbcMetaData.getPrecision(column);
    }

    public int getScale(int column)
        throws SQLException
    {
        return jdbcMetaData.getScale(column);
    }

    public String getTableName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getTableName(column));
    }

    public String getCatalogName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getCatalogName(column));
    }

    public int getColumnType(int column)
        throws SQLException
    {
        return jdbcMetaData.getColumnType(column);
    }

    public String getColumnTypeName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnTypeName(column));
    }

    public boolean isReadOnly(int column)
        throws SQLException
    {
        return jdbcMetaData.isReadOnly(column);
    }

    public boolean isWritable(int column)
        throws SQLException
    {
        return jdbcMetaData.isWritable(column);
    }

    public boolean isDefinitelyWritable(int column)
        throws SQLException
    {
        return jdbcMetaData.isDefinitelyWritable(column);
    }

    public String getColumnClassName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnClassName(column));
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return jdbcMetaData.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return jdbcMetaData.isWrapperFor(iface);
    }
}
