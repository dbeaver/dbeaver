/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

public class ClickhouseDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(ClickhouseDataSource.class);

    public ClickhouseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new ClickhouseSQLDialect());
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        if (String.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "String");
        } else if (Integer.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Int32");
        } else if (Log.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Int64");
        } else if (Short.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Int16");
        } else if (Byte.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Int8");
        } else if (Float.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Float32");
        } else if (Double.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "Float64");
        } else if (Date.class.getName().equals(typeFullName)) {
            return resolveDataType(monitor, "DateTime");
        }
        return super.resolveDataType(monitor, typeFullName);
    }
}
