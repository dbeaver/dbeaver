/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreServerType;
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreTable base
 */
public abstract class PostgreTableReal extends PostgreTableBase
{
    private static final Log log = Log.getLog(PostgreTableReal.class);
    public static final String CAT_STATISTICS = "Statistics";

    private long rowCountEstimate;
    private Long rowCount;
    private Long diskSpace;
    final TriggerCache triggerCache = new TriggerCache();

    protected PostgreTableReal(PostgreSchema catalog)
    {
        super(catalog);
    }

    protected PostgreTableReal(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);

        this.rowCountEstimate = JDBCUtils.safeGetLong(dbResult, "reltuples");
    }

    // Copy constructor
    public PostgreTableReal(PostgreSchema container, DBSEntity source, boolean persisted) {
        super(container, source, persisted);

        // Copy triggers
        if (source instanceof PostgreTableReal) {

        }
    }

    @Property(category = CAT_STATISTICS, viewable = true, order = 22)
    public long getRowCountEstimate() {
        return rowCountEstimate;
    }

    @Property(category = CAT_STATISTICS, viewable = false, expensive = true, order = 23)
    public synchronized Long getRowCount(DBRProgressMonitor monitor)
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

    @Property(category = CAT_STATISTICS, viewable = false, expensive = true, order = 24)
    public synchronized Long getDiskSpace(DBRProgressMonitor monitor)
    {
        if (diskSpace != null) {
            return diskSpace;
        }
        if (!isPersisted() || this instanceof PostgreView || !getDataSource().isServerVersionAtLeast(8, 1)) {
            // Do not count rows for views
            return null;
        }
        if (getDataSource().getServerType() == PostgreServerType.COCKROACH) {
            return null;
        }

        // Query disk size
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Calculate relation size on disk")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement("select pg_total_relation_size(?)")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        diskSpace = dbResult.getLong(1);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Can't fetch disk space", e);
        }
        if (diskSpace == null) {
            diskSpace = -1L;
        }

        return diskSpace;
    }

    @Override
    public Collection<PostgreTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableConstraint.class);
    }

    public PostgreTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getSchema().constraintCache.getObject(monitor, getSchema(), this, ukName);
    }

    @Association
    public Collection<PostgreTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public PostgreTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    class TriggerCache extends JDBCObjectCache<PostgreTableReal, PostgreTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableReal owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT x.oid,x.*,p.pronamespace as func_schema_id" +
                "\nFROM pg_catalog.pg_trigger x" +
                "\nLEFT OUTER JOIN pg_catalog.pg_proc p ON p.oid=x.tgfoid " +
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

}
