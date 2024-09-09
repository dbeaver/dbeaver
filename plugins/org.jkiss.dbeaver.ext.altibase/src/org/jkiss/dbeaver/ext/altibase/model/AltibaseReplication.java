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
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AltibaseReplication extends AltibaseGlobalObject 
    implements DBSObjectLazy<AltibaseDataSource>, DBPRefreshableObject, DBPScriptObject, DBPStatefulObject, DBPObjectStatistics {

    private final SenderCache senderCache = new SenderCache();
    private final ReceiverCache receiverCache = new ReceiverCache();

    private String ddl;

    private String name;
    private String remoteAddr;
    private String remoteConnType;

    private boolean isStarted;
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

        isStarted = JDBCUtils.safeGetBoolean(resultSet, "IS_STARTED", "1");
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
    @Association
    public Collection<AltibaseReplicationSender> getReplicationSenders(@NotNull DBRProgressMonitor monitor) throws DBException {
        return senderCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    public List<AltibaseReplicationReceiver> getReplicationReceivers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return receiverCache.getAllObjects(monitor, this);
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
    public boolean getIsStarted() {
        return isStarted;
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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        ddl = null;

        return this.getDataSource().getReplicationCache().refreshObject(monitor, getDataSource(), this);
    }

    @Nullable
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

    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(ddl)) {
            ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getReplicationDDL(monitor, this, options) + ";";
        }

        return ddl;
    }

    @Override
    public DBSObjectState getObjectState() {
        return isStarted ? DBSObjectState.ACTIVE : DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of replication '" + this.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT status FROM system_.sys_users_ u, system_.sys_procedures_ p"
                            + " WHERE u.user_id = p.user_id AND u.user_name = ? AND proc_name = ?")) {
                dbStat.setString(1, getName());

                dbStat.executeStatement();

                try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                    if (dbResult != null && dbResult.next()) {
                        isStarted = JDBCUtils.safeGetBoolean(dbResult, 1, "1");
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    static class SenderCache extends JDBCObjectCache<AltibaseReplication, AltibaseReplicationSender> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull AltibaseReplication owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT s.*, (g.rep_gap * p.value1) as gap_size_in_byte FROM v$repsender s, v$repgap g,  v$property p "
                            + "WHERE s.rep_name = ? AND s.rep_name = g.rep_name AND p.name = 'REPLICATION_GAP_UNIT'");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected AltibaseReplicationSender fetchObject(@NotNull JDBCSession session, @NotNull AltibaseReplication owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseReplicationSender(owner, resultSet);
        }
    }

    static class ReceiverCache extends JDBCObjectCache<AltibaseReplication, AltibaseReplicationReceiver> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
                @NotNull AltibaseReplication owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * from v$repreceiver WHERE rep_name = ?");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected AltibaseReplicationReceiver fetchObject(@NotNull JDBCSession session, @NotNull AltibaseReplication owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibaseReplicationReceiver(owner, resultSet);
        }
    }

    ///////////////////////////////////
    // Statistics

    @Override
    public boolean hasStatistics() {
        return true;
    }

    @Override
    public long getStatObjectSize() {
        long gap = 0;
        if (senderCache.getCacheSize() > 0) {
            gap = senderCache.getCachedObjects().get(0).getGapSizeInByte();
        }
        return gap;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }
}
