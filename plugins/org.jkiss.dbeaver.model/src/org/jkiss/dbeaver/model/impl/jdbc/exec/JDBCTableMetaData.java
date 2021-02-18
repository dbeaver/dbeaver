/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCEntityMetaData {

    private final JDBCResultSetMetaDataImpl resultSetMetaData;
    private final String catalogName;
    private final String schemaName;
    private final String tableName;
    private final List<JDBCColumnMetaData> columns = new ArrayList<>();

    JDBCTableMetaData(@NotNull JDBCResultSetMetaDataImpl resultSetMetaData, String catalogName, String schemaName, @NotNull String tableName)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public JDBCResultSetMetaDataImpl getResultSetMetaData()
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

    @Override
    public String toString() {
        return DBUtils.getSimpleQualifiedName(catalogName, schemaName, tableName);
    }

    @Override
    public int hashCode() {
        return (catalogName == null ? 1 : catalogName.hashCode()) *
            (schemaName == null ? 2 : schemaName.hashCode()) *
            tableName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JDBCTableMetaData)) {
            return false;
        }
        JDBCTableMetaData md2 = (JDBCTableMetaData) obj;
        return CommonUtils.equalObjects(catalogName, md2.catalogName) &&
            CommonUtils.equalObjects(schemaName, md2.schemaName) &&
            CommonUtils.equalObjects(tableName, md2.tableName);
    }
}
