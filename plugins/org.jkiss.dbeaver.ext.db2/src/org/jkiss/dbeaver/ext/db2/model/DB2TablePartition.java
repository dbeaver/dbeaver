/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablePartitionAccessMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablePartitionStatus;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 Table Partition
 * 
 * @author Denis Forveille
 */
public class DB2TablePartition extends DB2Object<DB2Table> {

    private Integer partitionObjectId;

    private DB2Tablespace tablespace;
    private DB2Tablespace indexTablespace;
    private DB2Tablespace longTablespace;

    private DB2TablePartitionAccessMode accessMode;
    private DB2TablePartitionStatus status;
    private Integer seqNo;
    private Boolean lowInclusive;
    private String lowValue;
    private Boolean highInclusive;
    private String highValue;

    private Timestamp statsTime;
    private Long card;
    private Long nPages;
    private Long fPages;
    private Long overFLow;

    private Date lastUsed;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2TablePartition(DB2Table db2Table, ResultSet dbResult) throws DBException
    {
        super(db2Table, JDBCUtils.safeGetString(dbResult, "DATAPARTITIONNAME"), JDBCUtils.safeGetInt(dbResult, "DATAPARTITIONID"),
            true);

        DB2DataSource db2DataSource = db2Table.getDataSource();

        this.partitionObjectId = JDBCUtils.safeGetInteger(dbResult, "PARTITIONOBJECTID");

        this.accessMode = CommonUtils.valueOf(DB2TablePartitionAccessMode.class, JDBCUtils.safeGetString(dbResult, "ACCESS_MODE"));
        this.status = CommonUtils.valueOf(DB2TablePartitionStatus.class, JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.seqNo = JDBCUtils.safeGetInteger(dbResult, "SEQNO");
        this.lowInclusive = JDBCUtils.safeGetBoolean(dbResult, "LOWINCLUSIVE", DB2YesNo.Y.name());
        this.lowValue = JDBCUtils.safeGetString(dbResult, "LOWVALUE");
        this.highInclusive = JDBCUtils.safeGetBoolean(dbResult, "HIGHINCLUSIVE", DB2YesNo.Y.name());
        this.highValue = JDBCUtils.safeGetString(dbResult, "HIGHVALUE");

        if (db2DataSource.isAtLeastV9_5()) {

        }

        // Lookup tablespaces
        Integer tablespaceId = JDBCUtils.safeGetInteger(dbResult, "TBSPACEID");
        this.tablespace = DB2Utils.findTablespaceById(new VoidProgressMonitor(), db2Table.getDataSource(), tablespaceId);
        Integer longTablespaceId = JDBCUtils.safeGetInteger(dbResult, "LONG_TBSPACEID");
        this.indexTablespace = DB2Utils
            .findTablespaceById(new VoidProgressMonitor(), db2Table.getDataSource(), longTablespaceId);
        if (db2DataSource.isAtLeastV9_7()) {
            this.statsTime = JDBCUtils.safeGetTimestamp(dbResult, "STATS_TIME");
            this.card = JDBCUtils.safeGetLongNullable(dbResult, "CARD");
            this.nPages = JDBCUtils.safeGetLongNullable(dbResult, "NPAGES");
            this.fPages = JDBCUtils.safeGetLongNullable(dbResult, "FPAGES");
            this.overFLow = JDBCUtils.safeGetLongNullable(dbResult, "OVERFLOW");
            this.lastUsed = JDBCUtils.safeGetDate(dbResult, "LASTUSED");
            Integer indexTablespaceId = JDBCUtils.safeGetInteger(dbResult, "INDEX_TBSPACEID");
            this.longTablespace = DB2Utils.findTablespaceById(new VoidProgressMonitor(), db2Table.getDataSource(),
                indexTablespaceId);
        }
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public DB2Table getTable()
    {
        return parent;
    }

    @Property(viewable = false, order = 3)
    public Integer getPartitionObjectId()
    {
        return partitionObjectId;
    }

    @Property(viewable = true, order = 4, category = DB2Constants.CAT_TABLESPACE)
    public DB2Tablespace getTablespace()
    {
        return tablespace;
    }

    @Property(viewable = false, order = 5, category = DB2Constants.CAT_TABLESPACE)
    public DB2Tablespace getIndexTablespace()
    {
        return indexTablespace;
    }

    @Property(viewable = false, order = 6, category = DB2Constants.CAT_TABLESPACE)
    public DB2Tablespace getLongTablespace()
    {
        return longTablespace;
    }

    @Property(viewable = true, order = 7)
    public DB2TablePartitionAccessMode getAccessMode()
    {
        return accessMode;
    }

    @Property(viewable = true, order = 8)
    public DB2TablePartitionStatus getStatus()
    {
        return status;
    }

    @Property(viewable = true, order = 9)
    public Integer getSeqNo()
    {
        return seqNo;
    }

    @Property(viewable = true, order = 10)
    public Boolean getLowInclusive()
    {
        return lowInclusive;
    }

    @Property(viewable = true, order = 11)
    public String getLowValue()
    {
        return lowValue;
    }

    @Property(viewable = true, order = 12)
    public Boolean getHighInclusive()
    {
        return highInclusive;
    }

    @Property(viewable = true, order = 13)
    public String getHighValue()
    {
        return highValue;
    }

    @Property(viewable = false, order = 20, category = DB2Constants.CAT_STATS)
    public Timestamp getStatsTime()
    {
        return statsTime;
    }

    @Property(viewable = false, order = 21, category = DB2Constants.CAT_STATS)
    public Long getCard()
    {
        return card;
    }

    @Property(viewable = false, order = 22, category = DB2Constants.CAT_STATS)
    public Long getnPages()
    {
        return nPages;
    }

    @Property(viewable = false, order = 23, category = DB2Constants.CAT_STATS)
    public Long getfPages()
    {
        return fPages;
    }

    @Property(viewable = false, order = 24, category = DB2Constants.CAT_STATS)
    public Long getOverFLow()
    {
        return overFLow;
    }

    @Property(viewable = false, order = 40)
    public Date getLastUsed()
    {
        return lastUsed;
    }

}
