/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableRefreshMode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 MQT
 * 
 * @author Denis Forveille
 */
public class DB2MaterializedQueryTable extends DB2ViewBase implements DB2SourceObject {

    private DB2TableRefreshMode refreshMode;
    private Timestamp refreshTime;

    // -----------------
    // Constructors
    // -----------------

    public DB2MaterializedQueryTable(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(monitor, schema, dbResult);

        this.refreshTime = JDBCUtils.safeGetTimestamp(dbResult, "REFRESH_TIME");
        this.refreshMode = CommonUtils.valueOf(DB2TableRefreshMode.class, JDBCUtils.safeGetString(dbResult, "REFRESH"));

    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean isView()
    {
        return true; // DF: Not sure of that..
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getMaterializedQueryTableCache().clearChildrenCache(this);
        super.refreshObject(monitor);
        return true;
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2MaterializedQueryTable, DB2TableColumn> getCache()
    {
        return getContainer().getMaterializedQueryTableCache();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public Collection<DB2TableColumn> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getMaterializedQueryTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public DB2TableColumn getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException
    {
        return getContainer().getMaterializedQueryTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, editable = false, order = 102)
    public DB2TableRefreshMode getRefreshMode()
    {
        return refreshMode;
    }

    @Property(viewable = true, editable = false, order = 103)
    public Timestamp getRefreshTime()
    {
        return refreshTime;
    }

}
