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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;

/**
 * Oracle materialized view
 */
public class OracleMaterializedView extends OracleSchemaObject implements OracleSourceObject, DBSObjectLazy<OracleDataSource>
{

    private Object container;
    private String query;
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

    public OracleMaterializedView(OracleSchema schema, String name)
    {
        super(schema, name, false);
    }

    public OracleMaterializedView(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(
            schema,
            JDBCUtils.safeGetString(dbResult, "MVIEW_NAME"),
            true);
        this.query = JDBCUtils.safeGetString(dbResult, "QUERY");
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "COMPILE_STATE"));
        this.container = JDBCUtils.safeGetString(dbResult, "CONTAINER_NAME");
        this.updatable = JDBCUtils.safeGetBoolean(dbResult, "UPDATABLE", "Y");
        this.rewriteEnabled = JDBCUtils.safeGetBoolean(dbResult, "REWRITE_ENABLED", "Y");
        this.rewriteCapability = JDBCUtils.safeGetString(dbResult, "REWRITE_CAPABILITY");
        this.refreshMode = JDBCUtils.safeGetString(dbResult, "REFRESH_MODE");
        this.refreshMethod = JDBCUtils.safeGetString(dbResult, "REFRESH_METHOD");
        this.buildMode = JDBCUtils.safeGetString(dbResult, "BUILD_MODE");
        this.fastRefreshable = JDBCUtils.safeGetString(dbResult, "FAST_REFRESHABLE");
        this.lastRefreshType = JDBCUtils.safeGetString(dbResult, "LAST_REFRESH_TYPE");
        this.lastRefreshDate = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REFRESH_DATE");
        this.staleness = JDBCUtils.safeGetString(dbResult, "STALENESS");
    }

    @Property(viewable = true, order = 10)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getContainer(DBRProgressMonitor monitor) throws DBException
    {
        return OracleUtils.resolveLazyReference(monitor, getSchema(), getSchema().tableCache, this, "container");
    }

    @Property(viewable = true, order = 14)
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

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.MATERIALIZED_VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
    {
        return query;
    }

    public void setObjectDefinitionText(String source)
    {
        this.query = source;
    }

    @Override
    public DBEPersistAction[] getCompileActions()
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
        this.valid = OracleUtils.getObjectStatus(monitor, this, OracleObjectType.PACKAGE);
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return container;
    }
}
