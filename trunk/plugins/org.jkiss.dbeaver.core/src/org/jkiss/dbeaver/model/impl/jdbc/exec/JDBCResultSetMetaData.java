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
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
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
    private ResultSetMetaData original;
    private List<DBCAttributeMetaData> columns = new ArrayList<DBCAttributeMetaData>();
    private Map<String, JDBCTableMetaData> tables = new HashMap<String, JDBCTableMetaData>();

    public JDBCResultSetMetaData(JDBCResultSetImpl resultSet)
        throws DBCException
    {
        this.resultSet = resultSet;
        try {
            this.original = resultSet.getOriginal().getMetaData();
            int count = original.getColumnCount();
            for (int i = 1; i <= count; i++) {
                columns.add(createColumnMetaDataImpl(i));
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    protected JDBCColumnMetaData createColumnMetaDataImpl(int index) throws SQLException
    {
        return new JDBCColumnMetaData(this, index);
    }

    public JDBCResultSet getResultSet()
    {
        return resultSet;
    }

    public ResultSetMetaData getOriginal()
    {
        return original;
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
        StringBuilder fullName = new StringBuilder(tableName.length() + 32);
        if (!CommonUtils.isEmpty(catalogName)) fullName.append(catalogName).append("|");
        if (!CommonUtils.isEmpty(schemaName)) fullName.append(schemaName).append("|");
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

        StringBuilder fullName = new StringBuilder(64);
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
        return original.getColumnCount();
    }

    @Override
    public boolean isAutoIncrement(int column)
        throws SQLException
    {
        return original.isAutoIncrement(column);
    }

    @Override
    public boolean isCaseSensitive(int column)
        throws SQLException
    {
        return original.isCaseSensitive(column);
    }

    @Override
    public boolean isSearchable(int column)
        throws SQLException
    {
        return original.isSearchable(column);
    }

    @Override
    public boolean isCurrency(int column)
        throws SQLException
    {
        return original.isCurrency(column);
    }

    @Override
    public int isNullable(int column)
        throws SQLException
    {
        return original.isNullable(column);
    }

    @Override
    public boolean isSigned(int column)
        throws SQLException
    {
        return original.isSigned(column);
    }

    @Override
    public int getColumnDisplaySize(int column)
        throws SQLException
    {
        return original.getColumnDisplaySize(column);
    }

    @Override
    public String getColumnLabel(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getColumnLabel(column));
    }

    @Override
    public String getColumnName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getColumnName(column));
    }

    @Override
    public String getSchemaName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getSchemaName(column));
    }

    @Override
    public int getPrecision(int column)
        throws SQLException
    {
        return original.getPrecision(column);
    }

    @Override
    public int getScale(int column)
        throws SQLException
    {
        return original.getScale(column);
    }

    @Override
    public String getTableName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getTableName(column));
    }

    @Override
    public String getCatalogName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getCatalogName(column));
    }

    @Override
    public int getColumnType(int column)
        throws SQLException
    {
        return original.getColumnType(column);
    }

    @Override
    public String getColumnTypeName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getColumnTypeName(column));
    }

    @Override
    public boolean isReadOnly(int column)
        throws SQLException
    {
        return original.isReadOnly(column);
    }

    @Override
    public boolean isWritable(int column)
        throws SQLException
    {
        return original.isWritable(column);
    }

    @Override
    public boolean isDefinitelyWritable(int column)
        throws SQLException
    {
        return original.isDefinitelyWritable(column);
    }

    @Override
    public String getColumnClassName(int column)
        throws SQLException
    {
        return JDBCUtils.normalizeIdentifier(original.getColumnClassName(column));
    }

    @Override
    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return original.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return original.isWrapperFor(iface);
    }
}
