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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseTable base
 */
public abstract class KingbaseTableReal extends KingbaseTableBase implements DBPObjectStatistics
{
    private static final Log log = Log.getLog(KingbaseTableReal.class);

    protected long rowCountEstimate;
    protected transient volatile Long rowCount;
    protected transient volatile Long diskSpace;
    protected transient volatile long tableRelSize;
    private final TriggerCache triggerCache = new TriggerCache() ;
  
    public boolean isRefreshSchemaStatisticsOnTableRefresh () {
        return true;
    }

    protected KingbaseTableReal(KingbaseTableContainer container)
    {
        super(container);
    }

    protected KingbaseTableReal(
        KingbaseTableContainer container,
        ResultSet dbResult)
    {
        super(container, dbResult);

        this.rowCountEstimate = JDBCUtils.safeGetLong(dbResult, "reltuples");
    }

    // Copy constructor
    public KingbaseTableReal(DBRProgressMonitor monitor, KingbaseTableContainer container, KingbaseTableReal source, boolean persisted) throws DBException {
        super(monitor, container, source, persisted);

        for (KingbaseTableConstraint srcConstr : CommonUtils.safeCollection(source.getConstraints(monitor))) {
            KingbaseTableConstraint constr = new KingbaseTableConstraint(monitor, this, srcConstr);
            getSchema().getConstraintCache().cacheObject(constr);
        }
    }

    @Nullable
    public TriggerCache getTriggerCache() {
        return triggerCache;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = true, order = 22)
    public long getRowCountEstimate() {
        return rowCountEstimate;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, expensive = true, order = 23)
    public Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null) {
            return rowCount;
        }
        if (!isPersisted()) {
            // Do not count rows for views
            return null;
        }

        // Query row count
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read row count")) {
            rowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null, DBSDataContainer.FLAG_NONE);
        } catch (DBException e) {
            log.debug("Can't fetch row count", e);
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 24, formatter = ByteNumberFormat.class)
    public Long getDiskSpace(DBRProgressMonitor monitor)
    {
        readTableStats(monitor);

        return diskSpace;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 25, formatter = ByteNumberFormat.class)
    public long getRelationSize(DBRProgressMonitor monitor) {
        readTableStats(monitor);
        return tableRelSize;
    }

    @Override
    public boolean hasStatistics() {
        return diskSpace != null;
    }

    @Override
    public long getStatObjectSize() {
        return diskSpace == null ? 0 : diskSpace;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    private void readTableStats(DBRProgressMonitor monitor) {
        if (diskSpace != null ) {
            return;
        }
        if (!isPersisted() || this instanceof KingbaseView) {
            // Do not count rows for views
            return;
        }

        try {
            // Query disk size
            try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Calculate relation size on disk")) {
                readTableStatistics((JDBCSession) session);
            } catch (Exception e) {
                log.debug("Can't fetch disk space", e);
            }
        } finally {
            if (diskSpace == null) {
                diskSpace = -1L;
            }
        }
    }

    protected void readTableStatistics(JDBCSession session) throws DBException, SQLException {

        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "select " +
                    "sys_catalog.sys_total_relation_size(?) as total_rel_size," +
                    "sys_catalog.sys_relation_size(?) as rel_size"))
        {
            dbStat.setLong(1, getObjectId());
            dbStat.setLong(2, getObjectId());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    fetchStatistics(dbResult);
                }
            }
        }
    }

    protected void fetchStatistics(JDBCResultSet dbResult) throws DBException, SQLException {
        diskSpace = dbResult.getLong("total_rel_size");
        tableRelSize = dbResult.getLong("rel_size");
    }

    @Override
    public Collection<KingbaseTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().getConstraintCache().getTypedObjects(monitor, getSchema(), this, KingbaseTableConstraint.class);
    }

    public KingbaseTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getSchema().getConstraintCache().getObject(monitor, getSchema(), this, ukName);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.diskSpace != null && isRefreshSchemaStatisticsOnTableRefresh()) {
            // Re-read statistics on the next try
            getSchema().resetStatistics();
            this.diskSpace = null;
        }
        this.rowCount = null;
        this.tableRelSize = 0;

        return super.refreshObject(monitor);
    }

    @Nullable
    @Association
    public List<KingbaseTrigger> getTriggers(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache != null ? triggerCache.getAllObjects(monitor, this) : List.of();
    }

    @Nullable
    public KingbaseTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache != null ? triggerCache.getObject(monitor, this, name) : null;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    @NotNull
    @Override
    public DBSObjectType getObjectType() {
        return RelationalObjectType.TYPE_TABLE;
    }

    class TriggerCache extends JDBCObjectLookupCache<KingbaseTableReal, KingbaseTrigger> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull KingbaseTableReal owner, @Nullable KingbaseTrigger object, @Nullable String objectName) throws SQLException {
            String statement = "SELECT x.oid,x.*,p.pronamespace as func_schema_id,d.description" +
                "\nFROM sys_catalog.sys_trigger x" +
                "\nLEFT OUTER JOIN sys_catalog.sys_proc p ON p.oid=x.tgfoid " +
                "\nLEFT OUTER JOIN sys_catalog.sys_description d ON d.objoid=x.oid AND d.objsubid=0 " +
                "\nWHERE x.tgrelid = ?" +
                (object != null || CommonUtils.isNotEmpty(objectName) ? "\nAND x.tgname = ?" : "") +
                " AND NOT x.tgisinternal";
            JDBCPreparedStatement prepareStatement = session.prepareStatement(statement);
            prepareStatement.setLong(1, owner.getObjectId());
            if (object != null || CommonUtils.isNotEmpty(objectName)) {
                prepareStatement.setString(2, object != null ? object.getName() : objectName);
            }
            return prepareStatement;
        }

        @Override
        protected KingbaseTrigger fetchObject(@NotNull JDBCSession session, @NotNull KingbaseTableReal owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String name = JDBCUtils.safeGetString(dbResult, "tgname");
            if (CommonUtils.isEmpty(name)) {
                return null;
            }
            return new KingbaseTrigger(session.getProgressMonitor(), owner, name, dbResult);
        }

    }


}
