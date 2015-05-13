/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.fed;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Federated Nickname
 * 
 * @author Denis Forveille
 */
public class DB2Nickname extends DB2Table {

    private String remoteTableName;
    private String remoteSchemaName;
    private DB2RemoteServer db2RemoteServer;
    private DB2NicknameRemoteType remoteType;
    private Boolean cachingAllowed;

    // -----------------
    // Constructors
    // -----------------

    public DB2Nickname(DBRProgressMonitor monitor, DB2Schema db2Schema, ResultSet dbResult) throws DBException
    {
        super(monitor, db2Schema, dbResult);

        this.remoteTableName = JDBCUtils.safeGetString(dbResult, "REMOTE_TABLE");
        this.remoteSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "REMOTE_SCHEMA");
        this.remoteType = CommonUtils.valueOf(DB2NicknameRemoteType.class, JDBCUtils.safeGetString(dbResult, "REMOTE_TYPE"));
        this.cachingAllowed = JDBCUtils.safeGetBoolean(dbResult, "CACHINGALLOWED", DB2YesNo.Y.name());

        String serverName = JDBCUtils.safeGetString(dbResult, "SERVERNAME");
        if (serverName != null) {
            this.db2RemoteServer = getDataSource().getRemoteServer(monitor, serverName);
        }

    }

    @Override
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Messages.no_ddl_for_nicknames;
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, editable = false, order = 10)
    public String getRemoteSchemaName()
    {
        return remoteSchemaName;
    }

    @Property(viewable = true, editable = false, order = 11)
    public String getRemoteTableName()
    {
        return remoteTableName;
    }

    @Property(viewable = true, editable = false, order = 12)
    public DB2NicknameRemoteType getRemoteType()
    {
        return remoteType;
    }

    @Property(viewable = true, editable = false, order = 13)
    public DB2RemoteServer getDb2RemoteServer()
    {
        return db2RemoteServer;
    }

    @Property(viewable = true, editable = false, order = 14)
    public Boolean getCachingAllowed()
    {
        return cachingAllowed;
    }

}
