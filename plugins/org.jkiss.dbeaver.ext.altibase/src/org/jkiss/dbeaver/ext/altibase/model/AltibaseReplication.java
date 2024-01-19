/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.Timestamp;
import java.util.List;

public class AltibaseReplication extends AltibaseGlobalObject implements DBSObjectLazy<AltibaseDataSource>, DBPRefreshableObject {
    
    private String name;
    private String remoteAddr;
    private String remoteConnType;
    
    private String status;
    private String conflictResolution;
    private String mode;
    private String role;
    private int options;
    private String recoverable;
    private int parallelApplierCount;
    
    private long xsn;
    
    private Timestamp giveUpTime;
    private long giveUpXsn;
    private Timestamp remoteFaultDetectTime;
    
    protected AltibaseReplication(GenericStructContainer owner, JDBCResultSet resultSet) {
        super((AltibaseDataSource) owner.getDataSource(), true);
        
        name = JDBCUtils.safeGetString(resultSet, "REPLICATION_NAME");
        
        status = JDBCUtils.safeGetString(resultSet, "STATUS");
        conflictResolution = JDBCUtils.safeGetString(resultSet, "CONFLICT_RESOLUTION");
        mode = JDBCUtils.safeGetString(resultSet, "REPL_MODE");
        role = JDBCUtils.safeGetString(resultSet, "ROLE");
        options = JDBCUtils.safeGetInt(resultSet, "OPTIONS");
        recoverable = JDBCUtils.safeGetString(resultSet, "RECOVERABLE");
        parallelApplierCount = JDBCUtils.safeGetInt(resultSet, "PARALLEL_APPLIER_COUNT");
        
        xsn = JDBCUtils.safeGetLong(resultSet, "XSN");
        
        giveUpTime = JDBCUtils.safeGetTimestamp(resultSet, "GIVE_UP_TIME");
        giveUpXsn = JDBCUtils.safeGetLong(resultSet, "GIVE_UP_XSN");
        remoteFaultDetectTime = JDBCUtils.safeGetTimestamp(resultSet, "REMOTE_FAULT_DETECT_TIME");
        
        remoteAddr = JDBCUtils.safeGetString(resultSet, "REMOTE_ADDR");
        remoteConnType = JDBCUtils.safeGetString(resultSet, "REMOTE_CONN_TYPE");
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 2)
    public String getRemoteAddr() {
        return remoteAddr;
    }
    
    @Property(viewable = true, order = 3)
    public String getRemoteConnType() {
        return remoteConnType;
    }
    
    @Property(viewable = true, order = 10)
    public String getStatus() {
        return status;
    }
    
    @Property(viewable = true, order = 11)
    public String getConflictResolution() {
        return conflictResolution;
    }
    
    @Property(viewable = true, order = 12)
    public String getMode() {
        return mode;
    }
    
    @Property(viewable = true, order = 13)
    public String getRole() {
        return role;
    }
    
    @Property(viewable = true, order = 14)
    public int getOptions() {
        return options;
    }
    
    @Property(viewable = true, order = 15)
    public String getRecoverable() {
        return recoverable;
    }
    
    @Property(viewable = true, order = 16)
    public int getParallelApplierCount() {
        return parallelApplierCount;
    }
    
    @Property(viewable = true, order = 20)
    public long getXsn() {
        return xsn;
    }
    
    @Property(viewable = true, order = 30)
    public Timestamp getGiveUpTime() {
        return giveUpTime;
    }
    
    @Property(viewable = true, order = 31)
    public long getGiveUpXsn() {
        return giveUpXsn;
    }
    
    @Property(viewable = true, order = 32)
    public Timestamp getRemoteFaultDetectTime() {
        return remoteFaultDetectTime;
    }
    
    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return this.getDataSource().getReplicationCache().refreshObject(monitor, getDataSource(), this);
    }

    @Override
    public Object getLazyReference(Object propertyId) {
        return null;
    }
    
    /**
     * Returns a replication's children: replication item
     */
    public List<AltibaseReplicationItem> getReplicationItems(DBRProgressMonitor monitor) throws DBException {
        return this.getDataSource().getReplicationCache().getChildren(monitor, this.getDataSource(), this);
    }
}
