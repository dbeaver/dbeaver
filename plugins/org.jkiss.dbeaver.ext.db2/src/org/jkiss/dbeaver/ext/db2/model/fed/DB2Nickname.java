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
package org.jkiss.dbeaver.ext.db2.model.fed;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableAccessMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablePartitionMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableStatus;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Federated Nickname
 * 
 * @author Denis Forveille
 */
public class DB2Nickname extends DB2TableBase implements DBPNamedObject2, DBPRefreshableObject, DB2SourceObject {

    private DB2TableStatus status;

    private String dataCapture;
    private String constChecked;
    private DB2TablePartitionMode partitionMode;
    private DB2TableAccessMode accessMode;

    private Timestamp statsTime;
    private Long card;
    private Long nPages;
    private Long fPages;
    private Long overFLow;

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

        this.status = CommonUtils.valueOf(DB2TableStatus.class, JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.statsTime = JDBCUtils.safeGetTimestamp(dbResult, "STATS_TIME");

        this.dataCapture = JDBCUtils.safeGetString(dbResult, "DATACAPTURE");
        this.constChecked = JDBCUtils.safeGetString(dbResult, "CONST_CHECKED");
        this.partitionMode = CommonUtils.valueOf(DB2TablePartitionMode.class, JDBCUtils.safeGetString(dbResult, "PARTITION_MODE"));
        this.accessMode = CommonUtils.valueOf(DB2TableAccessMode.class, JDBCUtils.safeGetString(dbResult, "ACCESS_MODE"));

        this.card = JDBCUtils.safeGetLongNullable(dbResult, "CARD");
        this.nPages = JDBCUtils.safeGetLongNullable(dbResult, "NPAGES");
        this.fPages = JDBCUtils.safeGetLongNullable(dbResult, "FPAGES");
        this.overFLow = JDBCUtils.safeGetLongNullable(dbResult, "OVERFLOW");

        this.remoteTableName = JDBCUtils.safeGetString(dbResult, "REMOTE_TABLE");
        this.remoteSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "REMOTE_SCHEMA");
        this.remoteType = CommonUtils.valueOf(DB2NicknameRemoteType.class, JDBCUtils.safeGetString(dbResult, "REMOTE_TYPE"));
        this.cachingAllowed = JDBCUtils.safeGetBoolean(dbResult, "CACHINGALLOWED", DB2YesNo.Y.name());

        String serverName = JDBCUtils.safeGetString(dbResult, "SERVERNAME");
        if (serverName != null) {
            this.db2RemoteServer = getDataSource().getRemoteServer(monitor, serverName);
        }
    }

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2Nickname, DB2TableColumn> getCache()
    {
        return getContainer().getNicknameCache();
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);

        getContainer().getNicknameCache().clearChildrenCache(this);

        // DF: Clear base index/trigger cache. Not cheap but didn't found
        // another way..
        getContainer().getIndexCache().clearCache();

        return true;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return status.getState();
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Messages.no_ddl_for_nicknames;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 4)
    public DB2TableStatus getStatus()
    {
        return status;
    }

    @Property(viewable = false, editable = false, order = 100)
    public DB2TableAccessMode getAccessMode()
    {
        return accessMode;
    }

    @Property(viewable = true, editable = false, order = 101)
    public Boolean getCachingAllowed()
    {
        return cachingAllowed;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getCard()
    {
        return card;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Timestamp getStatsTime()
    {
        return statsTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getnPages()
    {
        return nPages;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getfPages()
    {
        return fPages;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getOverFLow()
    {
        return overFLow;
    }

    @Property(viewable = false, editable = false, order = 109)
    public String getDataCapture()
    {
        return dataCapture;
    }

    @Property(viewable = false, editable = false, order = 111)
    public DB2TablePartitionMode getPartitionMode()
    {
        return partitionMode;
    }

    @Property(viewable = false, editable = false, order = 111)
    public String getConstChecked()
    {
        return constChecked;
    }

    @Property(viewable = true, editable = false, order = 10, category = DB2Constants.CAT_REMOTE)
    public DB2RemoteServer getDb2RemoteServer()
    {
        return db2RemoteServer;
    }

    @Property(viewable = true, editable = false, order = 11, category = DB2Constants.CAT_REMOTE)
    public DB2NicknameRemoteType getRemoteType()
    {
        return remoteType;
    }

    @Property(viewable = true, editable = false, order = 12, category = DB2Constants.CAT_REMOTE)
    public String getRemoteSchemaName()
    {
        return remoteSchemaName;
    }

    @Property(viewable = true, editable = false, order = 13, category = DB2Constants.CAT_REMOTE)
    public String getRemoteTableName()
    {
        return remoteTableName;
    }

    // Hide TableId for nicknames

    @Property(viewable = false, hidden = true)
    public Integer getTableId()
    {
        return super.getTableId();
    }

}
