/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class GenericCatalog extends GenericObjectContainer implements DBSCatalog, DBSObjectSelector
{
    private String catalogName;
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

    public Collection<GenericSchema> getSchemas(DBRProgressMonitor monitor)
        throws DBException
    {
        if (schemas == null && !isInitialized) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this.getDataSource(), "Load catalog schemas")) {
                this.schemas = this.getDataSource().loadSchemas(session, this);
                this.isInitialized = true;
            }
        }
        return schemas;
    }

    public GenericSchema getSchema(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getSchemas(monitor), name);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
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
        return getDataSource().getContainer();
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

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        this.schemas = null;
        this.isInitialized = false;
        return this;
    }

    @Override
    public boolean supportsDefaultChange()
    {
        return GenericConstants.ENTITY_TYPE_SCHEMA.equals(getDataSource().getSelectedEntityType()) &&
            !CommonUtils.isEmpty(schemas);
    }

    @Override
    public GenericSchema getDefaultObject()
    {
        return DBUtils.findObject(schemas, getDataSource().getSelectedEntityName());
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException
    {
        final GenericSchema oldSelectedEntity = getDefaultObject();
        // Check removed because we can select the same object on invalidate
//        if (object == oldSelectedEntity) {
//            return;
//        }
        if (!(object instanceof GenericSchema)) {
            throw new DBException("Bad child type: " + object);
        }
        if (!schemas.contains(GenericSchema.class.cast(object))) {
            throw new DBException("Wrong child object specified as active: " + object);
        }

        GenericDataSource dataSource = getDataSource();
        for (JDBCExecutionContext context : dataSource.getAllContexts()) {
            dataSource.setActiveEntityName(monitor, context, object);
        }

        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        DBUtils.fireObjectSelect(object, true);
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        return getDataSource().refreshDefaultObject(session);
    }
}
