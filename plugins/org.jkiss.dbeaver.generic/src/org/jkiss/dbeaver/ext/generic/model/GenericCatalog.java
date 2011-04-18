/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class GenericCatalog extends GenericEntityContainer implements DBSCatalog, DBSEntitySelector
{
    private String catalogName;
    private List<GenericSchema> schemas;
    private boolean isInitialized = false;

    public GenericCatalog(GenericDataSource dataSource, String catalogName)
    {
        super(dataSource);
        this.catalogName = catalogName;
    }

    public GenericCatalog getCatalog()
    {
        return this;
    }

    public GenericSchema getSchema()
    {
        return null;
    }

    public GenericCatalog getObject()
    {
        return this;
    }

    public List<GenericSchema> getSchemas(DBRProgressMonitor monitor)
        throws DBException
    {
        if (schemas == null && !isInitialized) {
            JDBCExecutionContext context = this.getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load catalog schemas");
            try {
                this.schemas = this.getDataSource().loadSchemas(context, this);
                this.isInitialized = true;
            }
            finally {
                context.close();
            }
        }
        return schemas;
    }

    public GenericSchema getSchema(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getSchemas(monitor), name);
    }

    @Property(name = "Catalog Name", viewable = true, order = 1)
    public String getName()
    {
        return catalogName;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return getDataSource().getContainer();
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        if (CommonUtils.isEmpty(getSchemas(monitor))) {
            // Cache tables only if we don't have schemas
            super.cacheStructure(monitor, scope);
        }
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchemas(monitor);
        } else {
            return getTables(monitor);
        }
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchema(monitor, childName);
        } else {
            return super.getChild(monitor, childName);
        }
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return GenericSchema.class;
        } else {
            return GenericTable.class;
        }
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException {
        super.refreshEntity(monitor);
        this.schemas = null;
        this.isInitialized = false;
        return true;
    }

    public boolean supportsEntitySelect()
    {
        return GenericConstants.ENTITY_TYPE_SCHEMA.equals(getDataSource().getSelectedEntityType()) &&
            !CommonUtils.isEmpty(schemas);
    }

    public GenericSchema getSelectedEntity()
    {
        return DBUtils.findObject(schemas, getDataSource().getSelectedEntityName());
    }

    public void selectEntity(DBRProgressMonitor monitor, DBSEntity entity) throws DBException
    {
        final GenericSchema oldSelectedEntity = getSelectedEntity();
        if (entity == oldSelectedEntity) {
            return;
        }
        if (!(entity instanceof GenericSchema)) {
            throw new DBException("Bad child type: " + entity);
        }
        if (!schemas.contains(GenericSchema.class.cast(entity))) {
            throw new DBException("Wrong child object specified as active: " + entity);
        }

        getDataSource().setActiveEntityName(monitor, entity);

        if (oldSelectedEntity != null) {
            getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, oldSelectedEntity, false));
        }
        getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, entity, true));
    }
}
