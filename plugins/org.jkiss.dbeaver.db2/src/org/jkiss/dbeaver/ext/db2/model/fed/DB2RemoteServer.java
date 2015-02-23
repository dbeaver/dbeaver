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
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2GlobalObject;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2RemoteServerOptionCache;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * DB2 Federated Remote Server
 * 
 * @author Denis Forveille
 */
public class DB2RemoteServer extends DB2GlobalObject implements DBPRefreshableObject {

    private final DB2RemoteServerOptionCache optionsCache = new DB2RemoteServerOptionCache();

    private String name;
    private DB2Wrapper db2Wrapper;
    private String type;
    private String version;
    private String remarks;

    // -----------------
    // Constructors
    // -----------------

    public DB2RemoteServer(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        super(db2DataSource, true);

        this.name = JDBCUtils.safeGetString(dbResult, "SERVERNAME");
        this.type = JDBCUtils.safeGetString(dbResult, "SERVERTYPE");
        this.version = JDBCUtils.safeGetString(dbResult, "SERVERVERSION");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        String db2WrapperName = JDBCUtils.safeGetString(dbResult, "WRAPNAME");
        if (db2WrapperName != null) {
            this.db2Wrapper = getDataSource().getWrapper(VoidProgressMonitor.INSTANCE, db2WrapperName);
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        optionsCache.clearCache();
        return true;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2RemoteServerOption> getOptions(DBRProgressMonitor monitor) throws DBException
    {
        return optionsCache.getObjects(monitor, this);
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2Wrapper getDb2Wrapper()
    {
        return db2Wrapper;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getType()
    {
        return type;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getVersion()
    {
        return version;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getRemarks()
    {
        return remarks;
    }

}
