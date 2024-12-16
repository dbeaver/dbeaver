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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBPVirtualObject;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

/**
 * GenericSchema
 */
public class GenericSchema extends GenericObjectContainer implements DBSSchema, DBPSystemObject, DBPVirtualObject
{
    @Nullable
    private final GenericCatalog catalog;
    @NotNull
    private final String schemaName;
    private boolean virtualSchema;

    public GenericSchema(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName)
    {
        super(dataSource);
        this.catalog = catalog;
        this.schemaName = schemaName;
    }

    @Nullable
    @Override
    @Property(optional = true, order = 2, labelProvider = GenericCatalog.CatalogNameTermProvider.class)
    public GenericCatalog getCatalog()
    {
        return catalog;
    }

    @Override
    public GenericSchema getSchema()
    {
        return this;
    }

    @Override
    public GenericSchema getObject()
    {
        return this;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, labelProvider = SchemaNameTermProvider.class)
    public String getName()
    {
        return schemaName;
    }

    @Nullable
    @Override
    //@Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return catalog != null ? catalog : getDataSource().getContainer();
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor)
        throws DBException
    {
        return GenericTable.class;
    }

    @Override
    public boolean isSystem() {
        return getDataSource().getMetaModel().isSystemSchema(this);
    }

    @Override
    public boolean isVirtual() {
        return virtualSchema;
    }

    public void setVirtual(boolean nullSchema) {
        this.virtualSchema = nullSchema;
    }

    public static class SchemaNameTermProvider implements IPropertyValueTransformer<DBSObject, String> {
        @Override
        public String transform(DBSObject object, String value) throws IllegalArgumentException {
            String schemaTerm = object.getDataSource().getInfo().getSchemaTerm();
            if (!CommonUtils.isEmpty(schemaTerm)) {
                return schemaTerm + " " + ModelMessages.model_navigator_Name;
            }
            return ModelMessages.model_navigator_Name;
        }
    }

}
