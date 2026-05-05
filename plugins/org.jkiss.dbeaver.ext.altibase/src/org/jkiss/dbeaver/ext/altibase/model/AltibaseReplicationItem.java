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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

/*
 * ReplicationItem is a pair of replication tables consisting of local and remote. 
 * Replication is possible not only at the table level but also at the table partition level.
 * 
 * A replication can have multiple replicationItems.
 */
public class AltibaseReplicationItem extends AltibaseObject<AltibaseReplication> implements DBSAlias {

    private String tableOid;
    
    private String localSchemaName;
    private String localTableName;
    private String localPartnName;
    
    private String remoteSchemaName;
    private String remoteTableName;
    private String remotePartnName;

    private boolean isPartitionedRepl;
    private long invalidMaxSn;

    private DBSObject localTable = null;

    protected AltibaseReplicationItem(AltibaseReplication parent, JDBCResultSet resultSet) {
        super(parent, DBUtils.getFullyQualifiedName(
                parent.getDataSource(),
                JDBCUtils.safeGetString(resultSet, "LOCAL_USER_NAME"), 
                JDBCUtils.safeGetString(resultSet, "LOCAL_TABLE_NAME"), 
                JDBCUtils.safeGetString(resultSet, "LOCAL_PARTITION_NAME")),
                true);

        tableOid = JDBCUtils.safeGetString(resultSet, "TABLE_OID");

        isPartitionedRepl = JDBCUtils.safeGetBoolean(resultSet, "REPLICATION_UNIT", "P");

        invalidMaxSn = JDBCUtils.safeGetLong(resultSet, "INVALID_MAX_SN");

        localSchemaName = JDBCUtils.safeGetString(resultSet, "LOCAL_USER_NAME");
        localTableName  = JDBCUtils.safeGetString(resultSet, "LOCAL_TABLE_NAME");
        localPartnName  = JDBCUtils.safeGetString(resultSet, "LOCAL_PARTITION_NAME");

        remoteSchemaName= JDBCUtils.safeGetString(resultSet, "REMOTE_USER_NAME");
        remoteTableName = JDBCUtils.safeGetString(resultSet, "REMOTE_TABLE_NAME");
        remotePartnName = JDBCUtils.safeGetString(resultSet, "REMOTE_PARTITION_NAME");
    }

    @NotNull
    @Override
    @Property(viewable = false, order = 1, hidden = true)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getTableOid() {
        return tableOid;
    }

    @NotNull
    @Property(viewable = true, linkPossible = true, order = 5)
    public AltibaseTable getReplObjFrom(DBRProgressMonitor monitor) throws DBException {
        if (localTable == null) {
            localTable = getTargetObject(monitor);
        }

        return (AltibaseTable) localTable;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public String getReplObjTo() {
        return DBUtils.getFullyQualifiedName(parent.getDataSource(), remoteSchemaName, remoteTableName, remotePartnName);
    }

    @Property(viewable = true, order = 10)
    public boolean getIsPartitionedRepl() {
        return isPartitionedRepl;
    }

    @Property(viewable = true, order = 11)
    public long getInvalidMaxSn() {
        return invalidMaxSn;
    }

    @NotNull
    @Override
    public DBSObject getTargetObject(@Nullable DBRProgressMonitor monitor) throws DBException {
        DBSObject localTable = null;
        AltibaseSchema refSchema = (AltibaseSchema) getDataSource().getSchema(localSchemaName);
        if (refSchema != null) {
            localTable = refSchema.getTable(monitor, localTableName);
        }
        return localTable;
    }
}
