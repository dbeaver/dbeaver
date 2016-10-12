/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.model;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.ExasolDataSourceProvider;
import org.jkiss.dbeaver.ext.exasol.ExasolSQLDialect;
import org.jkiss.dbeaver.ext.exasol.model.plan.ExasolPlanAnalyser;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExasolDataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);

    private static final String GET_CURRENT_USER = "SELECT CURRENT_USER";
    private static final String GET_CURRENT_SCHEMA = "SELECT CURRENT_SCHEMA";
    private static final String SET_CURRENT_SCHEMA = "OPEN SCHEMA \"%s\"";

    private static final String C_SCHEMA = "select b.object_name,owner,created,object_comment from EXA_ALL_OBJECTS b where b.object_type = 'SCHEMA' "
        + "union all select distinct SCHEMA_NAME as \"OBJECT_NAME\", 'SYS' as owner, cast(null as timestamp) as created, '' as \"OBJECT_COMMENT\" from SYS.EXA_SYSCAT "
        + "order by b.object_name";
    private static final String C_USERS = "select * from EXA_ALL_USERS";
    private static final String C_DT = "SELECT * FROM EXA_SQL_TYPES";


    private final DBSObjectCache<ExasolDataSource, ExasolSchema> schemaCache = new JDBCObjectSimpleCache<>(
        ExasolSchema.class, C_SCHEMA);


    private final DBSObjectCache<ExasolDataSource, ExasolUser> userCache = new JDBCObjectSimpleCache<>(ExasolUser.class, C_USERS);

    private final DBSObjectCache<ExasolDataSource, ExasolDataType> dataTypeCache = new JDBCObjectSimpleCache<>(ExasolDataType.class, C_DT);

/*	    private final DBSObjectCache<ExasolDataSource, ExasolConnection> exasolconnection = new JDBCObjectSimpleCache<>(
                ExasolConnection.class, C_SV); */


    private String activeSchemaName;

    // -----------------------
    // Constructors
    // -----------------------

    public ExasolDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container);
    }

    @Override
    protected boolean isConnectionReadOnlyBroken() {
        return true;
    }

    // -----------------------
    // Initialisation/Structure
    // -----------------------

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {

            // First try to get active schema from special register 'CURRENT SCHEMA'
            this.activeSchemaName = determineActiveSchema(session);

        } catch (SQLException e) {
            LOG.warn("Error reading active schema", e);
        }

        try {
            this.dataTypeCache.getAllObjects(monitor, this);
        } catch (DBException e) {
            LOG.warn("Error reading types info", e);
            this.dataTypeCache.setCache(Collections.<ExasolDataType>emptyList());
        }

    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        if (setActiveObject) {
            setCurrentSchema(monitor, context, getDefaultObject());
        }
    }

    private String determineActiveSchema(JDBCSession session) throws SQLException {
        // First try to get active schema from special register 'CURRENT SCHEMA'
        String defSchema = JDBCUtils.queryString(session, GET_CURRENT_SCHEMA);
        if (defSchema == null) {
            return "";
        }

        return defSchema.trim();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object getAdapter(Class adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return new ExasolStructureAssistant(this);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        // TODO DF: No idea what to do with this method, what it is used for...
    }

    // -----------------------
    // Connection related Info
    // -----------------------

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        return connectionInfo.getUserName();
    }

    @NotNull
    @Override
    public ExasolDataSource getDataSource() {
        return this;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData) {
        final ExasolDataSourceInfo info = new ExasolDataSourceInfo(metaData);


        info.setSupportsResultSetScroll(false);

        return info;
    }

    @Override
    protected SQLDialect createSQLDialect(@NotNull JDBCDatabaseMetaData metaData) {
        return new ExasolSQLDialect(metaData);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor) throws DBCException {
        Map<String, String> props = new HashMap<>();
        props.putAll(ExasolDataSourceProvider.getConnectionsProps());
        return props;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        this.userCache.clearCache();
        this.dataTypeCache.clearCache();

        this.initialize(monitor);

        return this;
    }


    // --------------------------
    // Manage Children: ExasolSchema
    // --------------------------

    @Override
    public boolean supportsDefaultChange() {
        return true;
    }

    @Override
    public Class<? extends ExasolSchema> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ExasolSchema.class;
    }

    @Override
    public Collection<ExasolSchema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public ExasolSchema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getSchema(monitor, childName);
    }

    @Override
    public ExasolSchema getDefaultObject() {
        return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException {
        final ExasolSchema oldSelectedEntity = getDefaultObject();

        if (!(object instanceof ExasolSchema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }

        for (JDBCExecutionContext context : getAllContexts()) {
            setCurrentSchema(monitor, context, (ExasolSchema) object);
        }

        activeSchemaName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeSchemaName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
        try {
            final String newSchemaName = determineActiveSchema((JDBCSession) session);
            if (!CommonUtils.equalObjects(newSchemaName, activeSchemaName)) {
                final ExasolSchema newSchema = schemaCache.getCachedObject(newSchemaName);
                if (newSchema != null) {
                    setDefaultObject(session.getProgressMonitor(), newSchema);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DBException(e, this);
        }
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, ExasolSchema object) throws DBCException {
        if (object == null) {
            LOG.debug("Null current schema");
            return;
        }
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            JDBCUtils.executeSQL(session, String.format(SET_CURRENT_SCHEMA, object.getName()));
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    // --------------
    // Associations
    // --------------

    @Association
    public Collection<ExasolSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    public ExasolSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        return schemaCache.getObject(monitor, this, name);
    }

    public Collection<ExasolUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    public ExasolUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<ExasolDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return dataTypeCache.getAllObjects(monitor, this);
    }

    public ExasolDataType getDataType(DBRProgressMonitor monitor, String name) throws DBException {
        return dataTypeCache.getObject(monitor, this, name);
    }

    // -------------
    // Dynamic Data
    // -------------


    // -------------------------
    // Standards Getters
    // -------------------------

    public DBSObjectCache<ExasolDataSource, ExasolUser> getUserCache() {
        return userCache;
    }

    public DBSObjectCache<ExasolDataSource, ExasolSchema> getSchemaCache() {
        return schemaCache;
    }


    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        try {
            return getDataTypes(VoidProgressMonitor.INSTANCE);
        } catch (DBException e) {
            LOG.error("DBException occured when reading system dataTypes: ", e);
            return null;
        }
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        try {
            return getDataType(VoidProgressMonitor.INSTANCE, typeName);
        } catch (DBException e) {
            LOG.error("DBException occured when reading system dataType: " + typeName, e);
            return null;
        }
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBCException {
        ExasolPlanAnalyser plan = new ExasolPlanAnalyser(this, query);
        plan.explain(session);
        return plan;
    }

    public DBSObjectCache<ExasolDataSource, ExasolDataType> getDataTypeCache() {
        return dataTypeCache;
    }


}
