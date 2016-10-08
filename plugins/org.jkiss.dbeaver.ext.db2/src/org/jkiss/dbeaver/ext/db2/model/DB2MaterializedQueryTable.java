/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableRefreshMode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 MQT
 * 
 * @author Denis Forveille
 */
public class DB2MaterializedQueryTable extends DB2ViewBase implements DB2SourceObject {

    private DB2TableRefreshMode refreshMode;
    private Timestamp           refreshTime;

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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getMaterializedQueryTableCache().clearChildrenCache(this);

        super.refreshObject(monitor);

        return getContainer().getMaterializedQueryTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2MaterializedQueryTable, DB2TableColumn> getCache()
    {
        return getContainer().getMaterializedQueryTableCache();
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
