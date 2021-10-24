/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

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
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * PostgreTable base
 */
public abstract class PostgreTableReal extends PostgreTableBase implements DBPObjectStatistics
{
    private static final Log log = Log.getLog(PostgreTableReal.class);

    protected long rowCountEstimate;
    protected transient volatile Long rowCount;
    protected transient volatile Long diskSpace;
    protected transient volatile long tableRelSize;
    private final TriggerCache triggerCache = new TriggerCache();
    private final RuleCache ruleCache = new RuleCache();

    protected PostgreTableReal(PostgreTableContainer container)
    {
        super(container);
    }

    protected PostgreTableReal(
        PostgreTableContainer container,
        ResultSet dbResult)
    {
        super(container, dbResult);

        this.rowCountEstimate = JDBCUtils.safeGetLong(dbResult, "reltuples");
    }

    // Copy constructor
    public PostgreTableReal(DBRProgressMonitor monitor, PostgreTableContainer container, PostgreTableReal source, boolean persisted) throws DBException {
        super(monitor, container, source, persisted);

        for (PostgreTableConstraint srcConstr : CommonUtils.safeCollection(source.getConstraints(monitor))) {
            PostgreTableConstraint constr = new PostgreTableConstraint(monitor, this, srcConstr);
            getSchema().getConstraintCache().cacheObject(constr);
        }
    }

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
        if (diskSpace != null || !getDataSource().getServerType().supportsTableStatistics()) {
            return;
        }
        if (!isPersisted() || this instanceof PostgreView || !getDataSource().isServerVersionAtLeast(8, 1)) {
            // Do not count rows for views
            return;
        }
        if (!getDataSource().getServerType().supportsRelationSizeCalc()) {
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
        if (!getDataSource().getServerType().supportsTableStatistics()) {
            return;
        }
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "select " +
                    "pg_catalog.pg_total_relation_size(?) as total_rel_size," +
                    "pg_catalog.pg_relation_size(?) as rel_size"))
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
    public Collection<PostgreTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().getConstraintCache().getTypedObjects(monitor, getSchema(), this, PostgreTableConstraint.class);
    }

    public PostgreTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getSchema().getConstraintCache().getObject(monitor, getSchema(), this, ukName);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.diskSpace != null) {
            // Re-read statistics on the next try
            getSchema().resetStatistics();
        }
        this.rowCount = null;
        this.diskSpace = null;
        this.tableRelSize = 0;

        return super.refreshObject(monitor);
    }

    @Nullable
    @Association
    public List<PostgreTrigger> getTriggers(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public PostgreTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<PostgreRule> getRules(DBRProgressMonitor monitor)
        throws DBException
    {
        return ruleCache.getAllObjects(monitor, this);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    class TriggerCache extends JDBCObjectCache<PostgreTableReal, PostgreTrigger> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableReal owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT x.oid,x.*,p.pronamespace as func_schema_id,d.description" +
                "\nFROM pg_catalog.pg_trigger x" +
                "\nLEFT OUTER JOIN pg_catalog.pg_proc p ON p.oid=x.tgfoid " +
                "\nLEFT OUTER JOIN pg_catalog.pg_description d ON d.objoid=x.oid AND d.objsubid=0 " +
                "\nWHERE x.tgrelid=" + owner.getObjectId() +
                (getDataSource().isServerVersionAtLeast(9, 0) ? " AND NOT x.tgisinternal" : ""));
        }

        @Override
        protected PostgreTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableReal owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreTrigger(session.getProgressMonitor(), owner, dbResult);
        }

    }

    class RuleCache extends JDBCObjectCache<PostgreTableReal, PostgreRule> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableReal owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT r.oid,r.*, pg_get_ruledef(r.oid) AS definition\n" +
                    "FROM pg_rewrite r\n" +
                    "WHERE r.ev_class=" + owner.getObjectId() + " AND r.rulename <> '_RETURN'::name");
        }

        @Override
        protected PostgreRule fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableReal owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreRule(session.getProgressMonitor(), owner, dbResult);
        }

    }

}
