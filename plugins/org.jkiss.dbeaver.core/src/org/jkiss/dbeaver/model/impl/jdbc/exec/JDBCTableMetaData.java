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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCEntityMetaData {

    private final JDBCResultSetMetaData resultSetMetaData;
    private final String catalogName;
    private final String schemaName;
    private final String tableName;
    private final List<JDBCColumnMetaData> columns = new ArrayList<JDBCColumnMetaData>();

    JDBCTableMetaData(@NotNull JDBCResultSetMetaData resultSetMetaData, String catalogName, String schemaName, @NotNull String tableName)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public JDBCResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    @Nullable
    public String getCatalogName() {
        return catalogName;
    }

    @Nullable
    public String getSchemaName() {
        return schemaName;
    }

    @NotNull
    @Override
    public String getEntityName()
    {
        return tableName;
    }

    @NotNull
    @Override
    public List<JDBCColumnMetaData> getAttributes()
    {
        return columns;
    }

    void addAttribute(JDBCColumnMetaData columnMetaData)
    {
        columns.add(columnMetaData);
    }

}
