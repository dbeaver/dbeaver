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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
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

    public GenericCatalog(GenericDataSource dataSource, String catalogName)
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
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        this.schemas = null;
        this.isInitialized = false;
        return true;
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return GenericConstants.ENTITY_TYPE_SCHEMA.equals(getDataSource().getSelectedEntityType()) &&
            !CommonUtils.isEmpty(schemas);
    }

    @Override
    public GenericSchema getSelectedObject()
    {
        return DBUtils.findObject(schemas, getDataSource().getSelectedEntityName());
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object) throws DBException
    {
        final GenericSchema oldSelectedEntity = getSelectedObject();
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
}
