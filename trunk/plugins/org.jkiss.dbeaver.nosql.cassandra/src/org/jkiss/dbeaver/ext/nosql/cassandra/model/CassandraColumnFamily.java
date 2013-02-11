/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.util.Collection;

/**
 * CassandraColumnFamily
 */
public class CassandraColumnFamily extends JDBCTable<CassandraDataSource, CassandraKeyspace> implements DBPRefreshableObject, DBPSystemObject
{
    static final Log log = LogFactory.getLog(CassandraColumnFamily.class);

    private String description;
    private Long rowCount;

    public CassandraColumnFamily(
        CassandraKeyspace container,
        String tableName,
        String remarks,
        boolean persisted)
    {
        super(container, tableName, persisted);
        this.description = remarks;
    }

    @Override
    public CassandraKeyspace.TableCache getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject getParentObject()
    {
        return getContainer();
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    public boolean isSystem()
    {
        return false;
    }

    @Property(viewable = true, order = 4)
    public CassandraKeyspace getSchema()
    {
        return getContainer();
    }

    @Override
    public synchronized Collection<CassandraColumn> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return this.getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public CassandraColumn getAttribute(DBRProgressMonitor monitor, String attributeName)
        throws DBException
    {
        return this.getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public synchronized Collection<CassandraIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().getIndexCache().getObjects(monitor, getContainer(), this);
    }

    @Override
    public synchronized Collection<DBSTableConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSTableForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Override
    public synchronized boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        this.getContainer().getTableCache().clearChildrenCache(this);
        this.getContainer().getIndexCache().clearObjectCache(this);
        rowCount = null;
        return true;
    }

    // Comment row count calculation - it works too long and takes a lot of resources without serious reason
    @Property(viewable = true, expensive = true, order = 5)
    public synchronized Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null) {
            return rowCount;
        }
        // Query row count
        DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Read row count");
        try {
            rowCount = countData(context, null);
        }
        catch (DBException e) {
            log.debug("Can't fetch row count: " + e.getMessage());
        }
        finally {
            context.close();
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

}
