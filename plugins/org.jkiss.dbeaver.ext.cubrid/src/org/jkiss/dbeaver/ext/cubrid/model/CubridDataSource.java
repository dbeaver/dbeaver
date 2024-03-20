/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubridDataSource extends GenericDataSource
{
    private final CubridMetaModel metaModel;
    private final CubridObjectContainer structureContainer;
    private boolean supportMultiSchema;

    public CubridDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, CubridMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new CubridSQLDialect());
        this.metaModel = new CubridMetaModel();
        this.structureContainer = new CubridObjectContainer(this);
    }

    @DPIContainer
    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return this;
    }

    public List<GenericSchema> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
        return this.getSchemas();
    }

    @Nullable
    @Override
    public GenericTableBase findTable(
            @NotNull DBRProgressMonitor monitor,
            @Nullable String catalogName,
            @Nullable String schemaName,
            @NotNull String tableName)
            throws DBException {
        String[] schema = tableName.split("\\.");
        if (schema.length > 1) {
            CubridUser user = (CubridUser) this.getSchema(schema[0].toUpperCase());
            return user.getTable(monitor, schema[1]);
        }
        return null;
    }

    @NotNull
    public CubridMetaModel getMetaModel() {
        return metaModel;
    }

    @Override
    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        Map<String, DBSDataType> types = new HashMap<>();
        for (DBSDataType dataType : super.getDataTypes(monitor)) {
            types.put(dataType.getName(), dataType);
        }
        return types.values();
    }

    public CubridObjectContainer getObjectContainer() {
        return structureContainer;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
    }

    public boolean getSupportMultiSchema() {
        return this.supportMultiSchema;
    }

    public void setSupportMultiSchema(boolean supportMultiSchema) {
        this.supportMultiSchema = supportMultiSchema;
    }
}
