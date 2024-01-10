/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * @author Shengkai Bai
 */
public class DamengDataFile implements DBSObject {

    private final DamengTablespace tablespace;

    private String name;

    private Timestamp createTime;

    private Status status;

    private Long totalSize;

    private Long freeSize;

    private Integer pageSize;

    private Long maxSize;

    private Boolean autoExtend;

    private Long nextSize;

    private String mirrorPath;

    private Long realFreeSize;


    public DamengDataFile(DamengTablespace tablespace, ResultSet dbResult) {
        this.tablespace = tablespace;
        this.name = JDBCUtils.safeGetString(dbResult, "PATH");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.status = JDBCUtils.safeGetInt(dbResult, DamengConstants.STATUS$) == 1 ? Status.ONLINE : Status.OFFLINE;
        this.pageSize = JDBCUtils.safeGetInt(dbResult, "PAGE_SIZE");
        this.totalSize = JDBCUtils.safeGetLong(dbResult, "TOTAL_SIZE") * pageSize;
        this.freeSize = JDBCUtils.safeGetLong(dbResult, "FREE_SIZE") * pageSize;
        this.autoExtend = JDBCUtils.safeGetInt(dbResult, "AUTO_EXTEND") == 1;
        this.maxSize = JDBCUtils.safeGetInt(dbResult, "MAX_SIZE") * 1024L * 1024L;
        this.nextSize = JDBCUtils.safeGetInt(dbResult, "NEXT_SIZE") * 1024L * 1024L;
        this.mirrorPath = JDBCUtils.safeGetString(dbResult, "MIRROR_PATH");
        this.realFreeSize = JDBCUtils.safeGetLong(dbResult, "REAL_FREE_SIZE") * pageSize;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return tablespace;
    }

    @Override
    public DBPDataSource getDataSource() {
        return tablespace.getDataSource();
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Long getTotalSize() {
        return totalSize;
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Long getFreeSize() {
        return freeSize;
    }

    @Property(viewable = true)
    public Boolean getAutoExtend() {
        return autoExtend;
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Long getMaxSize() {
        return maxSize;
    }

    @Property(viewable = true)
    public Status getStatus() {
        return status;
    }

    @Property(viewable = true)
    public Timestamp getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Integer getPageSize() {
        return pageSize;
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Long getNextSize() {
        return nextSize;
    }

    @Property(viewable = true)
    public String getMirrorPath() {
        return mirrorPath;
    }

    @Property(viewable = true, formatter = ByteNumberFormat.class)
    public Long getRealFreeSize() {
        return realFreeSize;
    }


    public enum Status {
        ONLINE,
        OFFLINE,
    }
}
