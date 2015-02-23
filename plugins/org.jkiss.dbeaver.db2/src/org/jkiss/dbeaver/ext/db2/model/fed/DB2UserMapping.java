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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.sql.ResultSet;

/**
 * DB2 Federated User Mapping
 * 
 * @author Denis Forveille
 */
public class DB2UserMapping extends DB2GlobalObject {

    private String authId;
    private DB2RemoteServer remoteServer;

    // -----------------
    // Constructors
    // -----------------

    public DB2UserMapping(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        super(db2DataSource, true);

        this.authId = JDBCUtils.safeGetStringTrimmed(dbResult, "AUTHID");

        String remoteServerName = JDBCUtils.safeGetStringTrimmed(dbResult, "SERVERNAME");
        remoteServer = db2DataSource.getRemoteServer(VoidProgressMonitor.INSTANCE, remoteServerName);

    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return authId;
    }

    @Property(viewable = true, order = 2)
    public DB2RemoteServer getRemoteServer()
    {
        return remoteServer;
    }

}
