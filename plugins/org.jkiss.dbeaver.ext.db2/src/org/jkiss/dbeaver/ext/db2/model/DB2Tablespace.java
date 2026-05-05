/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TablespaceContainerCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceDataType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Tablespace
 * 
 * @author Denis Forveille
 */
public class DB2Tablespace extends DB2GlobalObject implements DBPNamedObject, DBPRefreshableObject {

    private final DB2TablespaceContainerCache containerCache = new DB2TablespaceContainerCache();

    private String name;
    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private Integer tbspaceId;
    private DB2TablespaceType tbspaceType;
    private DB2TablespaceDataType dataType;
    private Integer extentSize;
    private Integer prefetchSize;
    private Double overHead;
    private Double transferRate;
    private Double writeOverHead;
    private Double writeTransferRate;
    private Integer pageSize;
    private String dbpgName;
    private Boolean dropRecovery;
    private Integer dataTag;
    private DB2StorageGroup storageGroup;
    private Integer effectivePrefetchSize;
    private String remarks;

    private DB2Bufferpool bufferpool;

    // -----------------------
    // Constructors
    // -----------------------

    // Constructor for lazy loading, acts as a placeholder.
    public DB2Tablespace(DB2DataSource db2DataSource, String db2TablespaceName) throws DBException
    {
        super(db2DataSource, false);
        this.name = db2TablespaceName;
    }

    public DB2Tablespace(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        super(db2DataSource, true);
        this.name = JDBCUtils.safeGetString(dbResult, "TBSPACE");
        this.owner = JDBCUtils.safeGetString(dbResult, DB2Constants.SYSCOLUMN_OWNER);
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, DB2Constants.SYSCOLUMN_CREATE_TIME);
        this.tbspaceId = JDBCUtils.safeGetInteger(dbResult, "TBSPACEID");
        this.tbspaceType = CommonUtils.valueOf(DB2TablespaceType.class, JDBCUtils.safeGetString(dbResult, "TBSPACETYPE"));
        this.dataType = CommonUtils.valueOf(DB2TablespaceDataType.class, JDBCUtils.safeGetString(dbResult, "DATATYPE"));
        this.extentSize = JDBCUtils.safeGetInteger(dbResult, "EXTENTSIZE");
        this.prefetchSize = JDBCUtils.safeGetInteger(dbResult, "PREFETCHSIZE");
        this.overHead = JDBCUtils.safeGetDouble(dbResult, "OVERHEAD");
        this.transferRate = JDBCUtils.safeGetDouble(dbResult, "TRANSFERRATE");
        this.pageSize = JDBCUtils.safeGetInteger(dbResult, "PAGESIZE");
        this.dbpgName = JDBCUtils.safeGetString(dbResult, "DBPGNAME");
        this.dropRecovery = JDBCUtils.safeGetBoolean(dbResult, "DROP_RECOVERY", DB2YesNo.Y.name());
        this.remarks = JDBCUtils.safeGetString(dbResult, DB2Constants.SYSCOLUMN_REMARKS);

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, DB2Constants.SYSCOLUMN_OWNER_TYPE));
        }
        if (db2DataSource.isAtLeastV9_5()) {
            this.writeOverHead = JDBCUtils.safeGetDouble(dbResult, "WRITEOVERHEAD");
            this.writeTransferRate = JDBCUtils.safeGetDouble(dbResult, "WRITETRANSFERRATE");
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.dataTag = JDBCUtils.safeGetInteger(dbResult, "DATATAG");
            this.effectivePrefetchSize = JDBCUtils.safeGetInteger(dbResult, "EFFECTIVEPREFETCHSIZE");
            this.writeOverHead = JDBCUtils.safeGetDouble(dbResult, "WRITEOVERHEAD");
            this.writeTransferRate = JDBCUtils.safeGetDouble(dbResult, "WRITETRANSFERRATE");

            String storageGroupName = JDBCUtils.safeGetString(dbResult, "SGNAME");
            if (storageGroupName != null) {
                this.storageGroup = db2DataSource.getStorageGroup(new VoidProgressMonitor(), storageGroupName);
            }
        }

        Integer bufferpoolId = JDBCUtils.safeGetInteger(dbResult, "BUFFERPOOLID");
        bufferpool = DB2Utils.findBufferpoolById(new VoidProgressMonitor(), db2DataSource, bufferpoolId);

    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        containerCache.clearCache();
        return this;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public Integer getTbspaceId()
    {
        return tbspaceId;
    }

    @Property(viewable = true, order = 3)
    public DB2Bufferpool getBufferPool()
    {
        return bufferpool;
    }

    @Property(viewable = true, order = 4)
    public DB2StorageGroup getStorageGroup()
    {
        return storageGroup;
    }

    @Property(viewable = true, order = 5)
    public Integer getPageSize()
    {
        return pageSize;
    }

    @Property(viewable = true, order = 6)
    public DB2TablespaceType getTbspaceType()
    {
        return tbspaceType;
    }

    @Property(viewable = true, order = 7)
    public DB2TablespaceDataType getDataType()
    {
        return dataType;
    }

    @Property(viewable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Integer getExtentSize()
    {
        return extentSize;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Integer getPrefetchSize()
    {
        return prefetchSize;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Double getOverHead()
    {
        return overHead;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Double getTransferRate()
    {
        return transferRate;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Double getWriteOverHead()
    {
        return writeOverHead;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Double getWriteTransferRate()
    {
        return writeTransferRate;
    }

    @Property(viewable = false, category = DB2Constants.CAT_PERFORMANCE)
    public Integer getEffectivePrefetchSize()
    {
        return effectivePrefetchSize;
    }

    @Property(viewable = false)
    public String getDbpgName()
    {
        return dbpgName;
    }

    @Property(viewable = false)
    public Boolean getDropRecovery()
    {
        return dropRecovery;
    }

    @Property(viewable = false)
    public Integer getDataTag()
    {
        return dataTag;
    }

    @Nullable
    @Override
    @Property(viewable = false, length = PropertyLength.MULTILINE)
    public String getDescription()
    {
        return remarks;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2TablespaceContainer> getContainers(DBRProgressMonitor monitor) throws DBException
    {
        return containerCache.getAllObjects(monitor, this);
    }

    public DB2TablespaceContainer getContainer(DBRProgressMonitor monitor, long containerId) throws DBException
    {
        for (DB2TablespaceContainer container : containerCache.getAllObjects(monitor, this)) {
            if (container.getContainerId() == containerId) {
                return container;
            }
        }
        return null;
    }

    static DB2Tablespace resolveTablespaceReference(DBRProgressMonitor monitor, DB2DataSource dataSource, Object reference)
        throws DBException
    {
        if (reference instanceof String) {
            return dataSource.getTablespaceCache().getObject(monitor, dataSource, (String) reference);
        }
        return (DB2Tablespace) reference;
    }

}
