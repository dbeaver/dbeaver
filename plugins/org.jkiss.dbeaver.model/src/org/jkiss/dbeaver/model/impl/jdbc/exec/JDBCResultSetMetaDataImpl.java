/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBCResultSetMetaDataImpl
 */
public class JDBCResultSetMetaDataImpl implements JDBCResultSetMetaData
{
    protected JDBCResultSet resultSet;
    protected ResultSetMetaData original;
    protected List<DBCAttributeMetaData> columns = new ArrayList<>();
    protected Map<String, JDBCTableMetaData> tables = new HashMap<>();

    public JDBCResultSetMetaDataImpl(JDBCResultSet resultSet)
        throws SQLException
    {
        this.resultSet = resultSet;
        this.original = resultSet.getOriginal().getMetaData();
        int count = original.getColumnCount();
        for (int i = 0; i < count; i++) {
            columns.add(createColumnMetaDataImpl(i));
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

    @Nullable
    public JDBCTableMetaData getTableMetaData(String catalogName, String schemaName, String tableName)
    {
        if (CommonUtils.isEmpty(tableName)) {
            // some constant instead of table name
            return null;
        }
        String fullQualifiedName = DBUtils.getSimpleQualifiedName(catalogName, schemaName, tableName);

        JDBCTableMetaData tableMetaData = tables.get(fullQualifiedName);
        if (tableMetaData == null) {
            tableMetaData = new JDBCTableMetaData(this, catalogName, schemaName, tableName);
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
