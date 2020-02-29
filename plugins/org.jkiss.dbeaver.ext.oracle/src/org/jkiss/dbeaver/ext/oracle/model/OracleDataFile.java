/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 * Oracle tablespace file
 */
public class OracleDataFile extends OracleObject<OracleTablespace> {

    public enum OnlineStatus {
        SYSOFF,
        SYSTEM,
        OFFLINE,
        ONLINE,
        RECOVER,
    }

    private final OracleTablespace tablespace;
    private long id;
    private long relativeNo;
    private BigDecimal bytes;
    private BigDecimal blocks;
    private BigDecimal maxBytes;
    private BigDecimal maxBlocks;
    private long incrementBy;
    private BigDecimal userBytes;
    private BigDecimal userBlocks;

    private boolean available;
    private boolean autoExtensible;
    private OnlineStatus onlineStatus;

    private boolean temporary;

    protected OracleDataFile(OracleTablespace tablespace, ResultSet dbResult, boolean temporary)
    {
        super(
            tablespace,
            JDBCUtils.safeGetString(dbResult, "FILE_NAME"),
            true);
        this.tablespace = tablespace;
        this.temporary = temporary;
        this.id = JDBCUtils.safeGetLong(dbResult, "FILE_ID");
        this.relativeNo = JDBCUtils.safeGetLong(dbResult, "RELATIVE_FNO");
        this.bytes = JDBCUtils.safeGetBigDecimal(dbResult, "BYTES");
        this.blocks = JDBCUtils.safeGetBigDecimal(dbResult, "BLOCKS");
        this.maxBytes = JDBCUtils.safeGetBigDecimal(dbResult, "MAXBYTES");
        this.maxBlocks = JDBCUtils.safeGetBigDecimal(dbResult, "MAXBLOCKS");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.userBytes = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BYTES");
        this.userBlocks = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BLOCKS");
        this.autoExtensible = JDBCUtils.safeGetBoolean(dbResult, "AUTOEXTENSIBLE", "Y");
        this.available = "AVAILABLE".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
        if (!this.temporary) {
            this.onlineStatus = CommonUtils.valueOf(OnlineStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "ONLINE_STATUS"));
        }
    }

    public OracleTablespace getTablespace()
    {
        return tablespace;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(order = 2)
    public long getId()
    {
        return id;
    }

    @Property(order = 3)
    public long getRelativeNo()
    {
        return relativeNo;
    }

    @Property(viewable = true, order = 4)
    public BigDecimal getBytes()
    {
        return bytes;
    }

    @Property(viewable = true, order = 5)
    public BigDecimal getBlocks()
    {
        return blocks;
    }

    @Property(viewable = true, order = 6)
    public BigDecimal getMaxBytes()
    {
        return maxBytes;
    }

    @Property(viewable = true, order = 7)
    public BigDecimal getMaxBlocks()
    {
        return maxBlocks;
    }

    @Property(viewable = true, order = 8)
    public long getIncrementBy()
    {
        return incrementBy;
    }

    @Property(viewable = true, order = 9)
    public BigDecimal getUserBytes()
    {
        return userBytes;
    }

    @Property(viewable = true, order = 10)
    public BigDecimal getUserBlocks()
    {
        return userBlocks;
    }

    @Property(viewable = true, order = 11)
    public boolean isAvailable()
    {
        return available;
    }

    @Property(viewable = true, order = 12)
    public boolean isAutoExtensible()
    {
        return autoExtensible;
    }

    @Property(order = 13)
    public OnlineStatus getOnlineStatus()
    {
        return onlineStatus;
    }

    @Property(order = 14)
    public boolean isTemporary()
    {
        return temporary;
    }

}
