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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * Oracle materialized view
 */
public class OracleMaterializedView extends OracleTableBase implements OracleSourceObject, DBSObjectLazy<OracleDataSource>
{
    private static final Log log = Log.getLog(OracleMaterializedView.class);

    public static class AdditionalInfo extends TableAdditionalInfo {
        private volatile boolean loaded = false;

        private boolean mviewValid;
        private Object container;
        private boolean updatable;
        private boolean rewriteEnabled;
        private boolean valid;
        private String rewriteCapability;
        private String refreshMode;
        private String refreshMethod;
        private String buildMode;
        private String fastRefreshable;
        private String lastRefreshType;
        private Date lastRefreshDate;
        private String staleness;

        @Property(viewable = false, order = 14)
        public boolean isUpdatable()
        {
            return updatable;
        }

        @Property(viewable = false, order = 15)
        public boolean isRewriteEnabled()
        {
            return rewriteEnabled;
        }

        @Property(viewable = false, order = 16)
        public String getRewriteCapability()
        {
            return rewriteCapability;
        }

        @Property(viewable = false, order = 17)
        public String getRefreshMode()
        {
            return refreshMode;
        }

        @Property(viewable = false, order = 18)
        public String getRefreshMethod()
        {
            return refreshMethod;
        }

        @Property(viewable = false, order = 19)
        public String getBuildMode()
        {
            return buildMode;
        }

        @Property(viewable = false, order = 20)
        public String getFastRefreshable()
        {
            return fastRefreshable;
        }

        @Property(viewable = false, order = 21)
        public String getLastRefreshType()
        {
            return lastRefreshType;
        }

        @Property(viewable = false, order = 22)
        public Date getLastRefreshDate()
        {
            return lastRefreshDate;
        }

        @Property(viewable = false, order = 23)
        public String getStaleness()
        {
            return staleness;
        }

    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    private String query;
    private OracleDDLFormat currentDDLFormat;

    public OracleMaterializedView(OracleSchema schema, String name)
    {
        super(schema, name, false);
    }

    public OracleMaterializedView(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    @Property(viewable = true, order = 10)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getContainer(DBRProgressMonitor monitor) throws DBException
    {
        return getAdditionalInfo(monitor).container;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.MATERIALIZED_VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
    {
        if (query == null) {
            currentDDLFormat = OracleDDLFormat.getCurrentFormat(getDataSource());
        }
        OracleDDLFormat newFormat = OracleDDLFormat.FULL;
        boolean isFormatInOptions = options.containsKey(OracleConstants.PREF_KEY_DDL_FORMAT);
        if (isFormatInOptions) {
            newFormat = (OracleDDLFormat) options.get(OracleConstants.PREF_KEY_DDL_FORMAT);
        }
        if (query == null || currentDDLFormat != newFormat && isPersisted()) {
            try {
                if (query == null || !isFormatInOptions) {
                    query = OracleUtils.getDDL(monitor, getTableTypeName(), this, currentDDLFormat, options);
                } else {
                    query = OracleUtils.getDDL(monitor, getTableTypeName(), this, newFormat, options);
                    currentDDLFormat = newFormat;
                }
            } catch (DBException e) {
                String message = e.getMessage();
                if (message != null) {
                    message = message.replace("*/", "* /");
                }
                query = "/*\nError generating materialized view DDL:\n" + message + "\n*/";
                log.warn("Error getting view definition from system package", e);
            }
        }
        return query;
    }

    public void setObjectDefinitionText(String source)
    {
        this.query = source;
    }

    public String getMViewText() {
        return query;
    }

    public void setCurrentDDLFormat(OracleDDLFormat currentDDLFormat) {
        this.currentDDLFormat = currentDDLFormat;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), getDataSource(), "MVIEWS") + " WHERE OWNER=? AND MVIEW_NAME=?")) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.mviewValid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "COMPILE_STATE"));
                        additionalInfo.container = JDBCUtils.safeGetString(dbResult, "CONTAINER_NAME");
                        additionalInfo.updatable = JDBCUtils.safeGetBoolean(dbResult, "UPDATABLE", "Y");
                        additionalInfo.rewriteEnabled = JDBCUtils.safeGetBoolean(dbResult, "REWRITE_ENABLED", "Y");
                        additionalInfo.rewriteCapability = JDBCUtils.safeGetString(dbResult, "REWRITE_CAPABILITY");
                        additionalInfo.refreshMode = JDBCUtils.safeGetString(dbResult, "REFRESH_MODE");
                        additionalInfo.refreshMethod = JDBCUtils.safeGetString(dbResult, "REFRESH_METHOD");
                        additionalInfo.buildMode = JDBCUtils.safeGetString(dbResult, "BUILD_MODE");
                        additionalInfo.fastRefreshable = JDBCUtils.safeGetString(dbResult, "FAST_REFRESHABLE");
                        additionalInfo.lastRefreshType = JDBCUtils.safeGetString(dbResult, "LAST_REFRESH_TYPE");
                        additionalInfo.lastRefreshDate = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REFRESH_DATE");
                        additionalInfo.staleness = JDBCUtils.safeGetString(dbResult, "STALENESS");
                    }
                    additionalInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @Override
    public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor)
    {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.MATERIALIZED_VIEW,
                "Compile materialized view",
                "ALTER MATERIALIZED VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this, OracleObjectType.MATERIALIZED_VIEW);
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return additionalInfo.container;
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName() {
        return "MATERIALIZED_VIEW";
    }

    protected String queryTableComment(JDBCSession session) throws SQLException {
        return JDBCUtils.queryString(
            session,
            "SELECT COMMENTS FROM ALL_MVIEW_COMMENTS WHERE OWNER=? AND MVIEW_NAME=?",
            getSchema().getName(),
            getName());
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().constraintCache.clearObjectCache(this);

        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

}
