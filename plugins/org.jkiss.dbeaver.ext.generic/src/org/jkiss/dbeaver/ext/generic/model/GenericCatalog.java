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
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class GenericCatalog extends GenericObjectContainer implements DBSCatalog
{
    private final String catalogName;
    private List<GenericSchema> schemas;
    private boolean isInitialized = false;

    public GenericCatalog(@NotNull GenericDataSource dataSource, @NotNull String catalogName)
    {
        super(dataSource);
        this.catalogName = catalogName;
    }

    @Override
    public GenericCatalog getCatalog()
    {
        return this;
    }

    @Override
    public GenericSchema getSchema()
    {
        return null;
    }

    @Override
    public GenericCatalog getObject()
    {
        return this;
    }

    public Collection<GenericSchema> getSchemaList(DBRProgressMonitor monitor)
        throws DBException
    {
        if (getDataSource().isMergeEntities()) {
            return null;
        }
        return getSchemas(monitor);
    }

    @Association
    public Collection<GenericSchema> getSchemas(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (schemas == null && !isInitialized && !monitor.isForceCacheUsage()) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load catalog schemas")) {
                this.schemas = this.getDataSource().getMetaModel().loadSchemas(session, getDataSource(), this);
                this.isInitialized = true;
            }
        }
        return schemas;
    }

    public GenericSchema getSchema(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(
            getSchemas(monitor),
            name,
            getDataSource().getSQLDialect().storesUnquotedCase() == DBPIdentifierCase.MIXED);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, labelProvider = CatalogNameTermProvider.class)
    public String getName()
    {
        return catalogName;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        if (CommonUtils.isEmpty(getSchemas(monitor))) {
            // Cache tables only if we don't have schemas
            super.cacheStructure(monitor, scope);
        }
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchemas(monitor);
        } else {
            return getTables(monitor);
        }
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchema(monitor, childName);
        } else {
            return super.getChild(monitor, childName);
        }
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(schemas) || (monitor != null && !CommonUtils.isEmpty(getSchemas(monitor)))) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.schemas = null;
        this.isInitialized = false;
        return super.refreshObject(monitor);
    }

    public static class CatalogNameTermProvider implements IPropertyValueTransformer<DBSObject, String> {
        @Override
        public String transform(DBSObject object, String value) throws IllegalArgumentException {
            String catalogTerm = object.getDataSource().getInfo().getCatalogTerm();
            if (!CommonUtils.isEmpty(catalogTerm)) {
                return catalogTerm + " " + ModelMessages.model_navigator_Name;
            }
            return ModelMessages.model_navigator_Name;
        }
    }
}
