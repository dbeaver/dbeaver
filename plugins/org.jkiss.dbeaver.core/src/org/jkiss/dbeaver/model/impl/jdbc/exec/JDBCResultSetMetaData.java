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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.utils.CommonUtils;

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
    private List<DBCAttributeMetaData> columns = new ArrayList<DBCAttributeMetaData>();
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

    @Override
    public DBCResultSet getResultSet()
    {
        return resultSet;
    }

    ResultSetMetaData getJdbcMetaData()
    {
        return jdbcMetaData;
    }

    @Override
    public List<DBCAttributeMetaData> getAttributes()
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

    @Override
    public int getColumnCount()
        throws SQLException
    {
        return jdbcMetaData.getColumnCount();
    }

    @Override
    public boolean isAutoIncrement(int column)
        throws SQLException
    {
        return jdbcMetaData.isAutoIncrement(column);
    }

    @Override
    public boolean isCaseSensitive(int column)
        throws SQLException
    {
        return jdbcMetaData.isCaseSensitive(column);
    }

    @Override
    public boolean isSearchable(int column)
        throws SQLException
    {
        return jdbcMetaData.isSearchable(column);
    }

    @Override
    public boolean isCurrency(int column)
        throws SQLException
    {
        return jdbcMetaData.isCurrency(column);
    }

    @Override
    public int isNullable(int column)
        throws SQLException
    {
        return jdbcMetaData.isNullable(column);
    }

    @Override
    public boolean isSigned(int column)
        throws SQLException
    {
        return jdbcMetaData.isSigned(column);
    }

    @Override
    public int getColumnDisplaySize(int column)
        throws SQLException
    {
        return jdbcMetaData.getColumnDisplaySize(column);
    }

    @Override
    public String getColumnLabel(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnLabel(column));
    }

    @Override
    public String getColumnName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnName(column));
    }

    @Override
    public String getSchemaName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getSchemaName(column));
    }

    @Override
    public int getPrecision(int column)
        throws SQLException
    {
        return jdbcMetaData.getPrecision(column);
    }

    @Override
    public int getScale(int column)
        throws SQLException
    {
        return jdbcMetaData.getScale(column);
    }

    @Override
    public String getTableName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getTableName(column));
    }

    @Override
    public String getCatalogName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getCatalogName(column));
    }

    @Override
    public int getColumnType(int column)
        throws SQLException
    {
        return jdbcMetaData.getColumnType(column);
    }

    @Override
    public String getColumnTypeName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnTypeName(column));
    }

    @Override
    public boolean isReadOnly(int column)
        throws SQLException
    {
        return jdbcMetaData.isReadOnly(column);
    }

    @Override
    public boolean isWritable(int column)
        throws SQLException
    {
        return jdbcMetaData.isWritable(column);
    }

    @Override
    public boolean isDefinitelyWritable(int column)
        throws SQLException
    {
        return jdbcMetaData.isDefinitelyWritable(column);
    }

    @Override
    public String getColumnClassName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(jdbcMetaData.getColumnClassName(column));
    }

    @Override
    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return jdbcMetaData.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return jdbcMetaData.isWrapperFor(iface);
    }
}
