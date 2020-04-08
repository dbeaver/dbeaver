/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ClickhouseDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(ClickhouseDataSource.class);

    private static Map<String, String> dataTypeMap = new HashMap<>();

    static {
        dataTypeMap.put(String.class.getName(), "String");
        dataTypeMap.put(Integer.class.getName(), "Int32");
        dataTypeMap.put(Long.class.getName(), "Int64");
        dataTypeMap.put(Short.class.getName(), "Int16");
        dataTypeMap.put(Byte.class.getName(), "Int8");
        dataTypeMap.put(Float.class.getName(), "Float32");
        dataTypeMap.put(Double.class.getName(), "Float64");
        dataTypeMap.put(Date.class.getName(), "DateTime");

    }

    public ClickhouseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new ClickhouseSQLDialect());
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        String shortName = dataTypeMap.get(typeFullName);
        if (shortName != null) {
            typeFullName = shortName;
        }

        return super.resolveDataType(monitor, typeFullName);
    }
}
