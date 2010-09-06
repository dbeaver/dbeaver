/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBUtils;

import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class GenericCatalog extends GenericEntityContainer implements DBSCatalog
{
    private GenericDataSource dataSource;
    private String catalogName;
    private List<GenericSchema> schemas;
    private boolean isInitialized = false;

    public GenericCatalog(GenericDataSource dataSource, String catalogName)
    {
        this.dataSource = dataSource;
        this.catalogName = catalogName;
        this.initCache();
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
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
            JDBCExecutionContext context = this.getDataSource().openContext(monitor);
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

    public GenericSchema getSchema(String name)
    {
        return DBUtils.findObject(schemas, name);
    }

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

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchemas(monitor);
        } else {
            return getTables(monitor);
        }
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(getSchemas(monitor))) {
            return getSchema(childName);
        } else {
            return super.getChild(monitor, childName);
        }
    }

    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
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
        return true;
    }
}
