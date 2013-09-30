/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2DataSourceProvider;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2StructureAssistant;
import org.jkiss.dbeaver.ext.db2.info.DB2Parameter;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2RemoteServer;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2UserMapping;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Wrapper;
import org.jkiss.dbeaver.ext.db2.model.plan.DB2PlanAnalyser;
import org.jkiss.dbeaver.ext.db2.model.security.DB2AuthIDType;
import org.jkiss.dbeaver.ext.db2.model.security.DB2Grantee;
import org.jkiss.dbeaver.ext.db2.model.security.DB2GranteeCache;
import org.jkiss.dbeaver.ext.db2.model.security.DB2Role;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DB2 DataSource
 * 
 * @author Denis Forveille
 */
public class DB2DataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {

    private static final Log LOG = LogFactory.getLog(DB2DataSource.class);

    private static final String GET_CURRENT_SCHEMA = "VALUES(CURRENT SCHEMA)";
    private static final String SET_CURRENT_SCHEMA = "SET CURRENT SCHEMA = %s";

    private static final String C_SCHEMA = "SELECT * FROM SYSCAT.SCHEMATA ORDER BY SCHEMANAME WITH UR";
    private static final String C_DT = "SELECT * FROM SYSCAT.DATATYPES WHERE METATYPE = 'S' ORDER BY TYPESCHEMA,TYPENAME WITH UR";
    private static final String C_BP = "SELECT * FROM SYSCAT.BUFFERPOOLS ORDER BY BPNAME WITH UR";
    private static final String C_TS = "SELECT * FROM SYSCAT.TABLESPACES ORDER BY TBSPACE WITH UR";
    private static final String C_RL = "SELECT * FROM SYSCAT.ROLES ORDER BY ROLENAME WITH UR";

    private static final String C_SV = "SELECT * FROM SYSCAT.SERVERS ORDER BY SERVERNAME WITH UR";
    private static final String C_WR = "SELECT * FROM SYSCAT.WRAPPERS ORDER BY WRAPNAME WITH UR";
    private static final String C_UM =
        "SELECT * FROM SYSCAT.USEROPTIONS WHERE OPTION = 'REMOTE_AUTHID' ORDER BY SERVERNAME,AUTHID WITH UR";

    private static final String PLAN_TABLE_TIT = "PLAN_TABLE missing";
    private static final String PLAN_TABLE_MIS = "EXPLAIN tables not found. Query can't be explained";
    private static final String PLAN_TABLE_MSG =
        "Tables for EXPLAIN not found in current schema nor in SYSTOOLS. Do you want DBeaver to create new EXPLAIN tables?";

    private final DBSObjectCache<DB2DataSource, DB2Schema> schemaCache = new JDBCObjectSimpleCache<DB2DataSource, DB2Schema>(
        DB2Schema.class, C_SCHEMA);
    private final DBSObjectCache<DB2DataSource, DB2DataType> dataTypeCache = new JDBCObjectSimpleCache<DB2DataSource, DB2DataType>(
        DB2DataType.class, C_DT);
    private final DBSObjectCache<DB2DataSource, DB2Bufferpool> bufferpoolCache =
        new JDBCObjectSimpleCache<DB2DataSource, DB2Bufferpool>(DB2Bufferpool.class, C_BP);
    private final DBSObjectCache<DB2DataSource, DB2Tablespace> tablespaceCache =
        new JDBCObjectSimpleCache<DB2DataSource, DB2Tablespace>(DB2Tablespace.class, C_TS);
    private final DBSObjectCache<DB2DataSource, DB2Role> roleCache = new JDBCObjectSimpleCache<DB2DataSource, DB2Role>(
        DB2Role.class, C_RL);

    private final DBSObjectCache<DB2DataSource, DB2RemoteServer> remoteServerCache =
        new JDBCObjectSimpleCache<DB2DataSource, DB2RemoteServer>(DB2RemoteServer.class, C_SV);
    private final DBSObjectCache<DB2DataSource, DB2Wrapper> wrapperCache = new JDBCObjectSimpleCache<DB2DataSource, DB2Wrapper>(
        DB2Wrapper.class, C_WR);
    private final DBSObjectCache<DB2DataSource, DB2UserMapping> userMappingCache =
        new JDBCObjectSimpleCache<DB2DataSource, DB2UserMapping>(DB2UserMapping.class, C_UM);

    private final DB2GranteeCache groupCache = new DB2GranteeCache(DB2AuthIDType.G);
    private final DB2GranteeCache userCache = new DB2GranteeCache(DB2AuthIDType.U);

    private List<DB2Parameter> listDBParameters;
    private List<DB2Parameter> listDBMParameters;

    private String activeSchemaName;
    private String planTableSchemaName;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2DataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container) throws DBException
    {
        super(monitor, container);
        getContainer().getPreferenceStore().setValue("db2.explain.plan.schema", "toto");
    }

    // -----------------------
    // Connection related Info
    // -----------------------

    @Override
    protected String getConnectionUserName(DBPConnectionInfo connectionInfo)
    {
        return connectionInfo.getUserName();
    }

    @Override
    public DB2DataSource getDataSource()
    {
        return this;
    }

    @Override
    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        final JDBCDataSourceInfo info = new JDBCDataSourceInfo(metaData);
        // TODO DF: Needs to be reviewed
        for (String kw : DB2Constants.ADVANCED_KEYWORDS) {
            info.addSQLKeyword(kw);
        }
        return info;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties()
    {
        return DB2DataSourceProvider.getConnectionsProps();
    }

    @Override
    public void initialize(DBRProgressMonitor monitor) throws DBException
    {
        super.initialize(monitor);

        {
            final JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Load data source meta info");
            try {
                // Get active schema
                this.activeSchemaName = JDBCUtils.queryString(context, GET_CURRENT_SCHEMA);
                if (this.activeSchemaName != null) {
                    this.activeSchemaName = this.activeSchemaName.trim();
                }

                listDBMParameters = DB2Utils.readDBMCfg(monitor, context);
                listDBParameters = DB2Utils.readDBCfg(monitor, context);

            } catch (SQLException e) {
                LOG.warn(e);
            } finally {
                context.close();
            }
        }

        this.dataTypeCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);

        this.userCache.clearCache();
        this.groupCache.clearCache();
        this.roleCache.clearCache();

        this.tablespaceCache.clearCache();
        this.bufferpoolCache.clearCache();
        this.schemaCache.clearCache();
        this.dataTypeCache.clearCache();

        this.remoteServerCache.clearCache();
        this.wrapperCache.clearCache();
        this.userMappingCache.clearCache();

        this.listDBMParameters = null;
        this.listDBParameters = null;

        this.initialize(monitor);

        return true;
    }

    @Override
    public Collection<DB2DataType> getDataTypes()
    {
        try {
            return getDataTypes(VoidProgressMonitor.INSTANCE);
        } catch (DBException e) {
            LOG.error("DBException occurred when reading system dataTypes: ", e);
            return null;
        }
    }

    @Override
    public DB2DataType getDataType(String typeName)
    {
        try {
            return getDataType(VoidProgressMonitor.INSTANCE, typeName);
        } catch (DBException e) {
            LOG.error("DBException occurred when reading system dataTYpe : " + typeName, e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new DB2StructureAssistant(this);
        }
        return null;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        // TODO DF: No idea what to do with this method, what it is used for...
    }

    // --------------------------
    // Manage Children: DB2Schema
    // --------------------------

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Override
    public Class<? extends DB2Schema> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Schema.class;
    }

    @Override
    public Collection<DB2Schema> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return getSchemas(monitor);
    }

    @Override
    public DB2Schema getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return getSchema(monitor, childName);
    }

    @Override
    public DB2Schema getSelectedObject()
    {
        return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object) throws DBException
    {
        final DB2Schema oldSelectedEntity = getSelectedObject();

        if (!(object instanceof DB2Schema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }

        activeSchemaName = object.getName();

        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.UTIL, "Set active schema");
        try {
            JDBCUtils.executeSQL(context, String.format(SET_CURRENT_SCHEMA, activeSchemaName));
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            context.close();
        }

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeSchemaName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    // -------
    // Helpers
    // -------

    /**
     * @param monitor
     * @param parentSchema
     * @param objectSchemaName
     * @return
     * @throws DBException
     */
    public DB2Schema schemaLookup(DBRProgressMonitor monitor, DB2Schema parentSchema, String objectSchemaName) throws DBException
    {
        LOG.debug("schemaLookup");

        // Quick bypass: If it's the same name (99% of the time), return the parentSchema
        if (parentSchema.getName().equals(objectSchemaName)) {
            return parentSchema;
        }

        // Lookup fo the schema that correspond to the name
        DB2Schema objectSchema = getSchema(monitor, objectSchemaName);
        if (objectSchema == null) {
            String msg = "Schema  '" + objectSchemaName + "' not found in database ??? Impossible";
            LOG.error(msg);
            throw new DBException(msg);

        }
        LOG.debug("objectSchemaName : " + objectSchemaName + " was different from parentSchema : " + parentSchema.getName());

        return objectSchema;
    }

    // --------------
    // Plan Tables
    // --------------

    @Override
    public DBCPlan planQueryExecution(DBCExecutionContext context, String query) throws DBCException
    {
        String ptSchemaname = getPlanTableSchemaName(context);
        if (ptSchemaname == null) {
            throw new DBCException(PLAN_TABLE_MIS);
        }
        DB2PlanAnalyser plan = new DB2PlanAnalyser(query, ptSchemaname);
        plan.explain((JDBCExecutionContext) context);
        return plan;
    }

    private String getPlanTableSchemaName(DBCExecutionContext context) throws DBCException
    {
        if (planTableSchemaName == null) {

            // TODO DF: not sure of "activeSchema".
            // Explain tables could be created in any schema or at default, in SYSTOOLS
            // Should be "CURRENT USER" in fact..
            planTableSchemaName = DB2Utils.checkExplainTables(context.getProgressMonitor(), this, activeSchemaName);

            if (planTableSchemaName == null) {
                // Plan table not found - try to create new one
                // TODO DF: ask the user in what schema to create the tables
                if (!UIUtils.confirmAction(DBeaverUI.getActiveWorkbenchShell(), PLAN_TABLE_TIT, PLAN_TABLE_MSG)) {
                    return null;
                }
                planTableSchemaName = "SYSTOOLS";
                DB2Utils.createExplainTables(context.getProgressMonitor(), this, planTableSchemaName);
            }
        }
        return planTableSchemaName;
    }

    // --------------
    // Associations
    // --------------

    @Association
    public Collection<DB2Schema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return schemaCache.getObjects(monitor, this);
    }

    public DB2Schema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2DataType> getDataTypes(DBRProgressMonitor monitor) throws DBException
    {
        return dataTypeCache.getObjects(monitor, this);
    }

    public DB2DataType getDataType(DBRProgressMonitor monitor, String name) throws DBException
    {
        return dataTypeCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Tablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException
    {
        return tablespaceCache.getObjects(monitor, this);
    }

    public DB2Tablespace getTablespace(DBRProgressMonitor monitor, String name) throws DBException
    {
        return tablespaceCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Bufferpool> getBufferpools(DBRProgressMonitor monitor) throws DBException
    {
        return bufferpoolCache.getObjects(monitor, this);
    }

    public DB2Bufferpool getBufferpool(DBRProgressMonitor monitor, String name) throws DBException
    {
        return bufferpoolCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Wrapper> getWrappers(DBRProgressMonitor monitor) throws DBException
    {
        return wrapperCache.getObjects(monitor, this);
    }

    public DB2Wrapper getWrapper(DBRProgressMonitor monitor, String name) throws DBException
    {
        return wrapperCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2UserMapping> getUserMappings(DBRProgressMonitor monitor) throws DBException
    {
        return userMappingCache.getObjects(monitor, this);
    }

    public DB2UserMapping getUserMapping(DBRProgressMonitor monitor, String name) throws DBException
    {
        return userMappingCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2RemoteServer> getRemoteServers(DBRProgressMonitor monitor) throws DBException
    {
        return remoteServerCache.getObjects(monitor, this);
    }

    public DB2RemoteServer getRemoteServer(DBRProgressMonitor monitor, String name) throws DBException
    {
        return remoteServerCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Grantee> getUsers(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getObjects(monitor, this);
    }

    public DB2Grantee getUser(DBRProgressMonitor monitor, String name) throws DBException
    {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Grantee> getGroups(DBRProgressMonitor monitor) throws DBException
    {
        return groupCache.getObjects(monitor, this);
    }

    public DB2Grantee getGroup(DBRProgressMonitor monitor, String name) throws DBException
    {
        return groupCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Role> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getObjects(monitor, this);
    }

    public DB2Role getRole(DBRProgressMonitor monitor, String name) throws DBException
    {
        return roleCache.getObject(monitor, this, name);
    }

    // -------------
    // Dynamic Data
    // -------------

    public List<DB2Parameter> getDbParameters(DBRProgressMonitor monitor) throws DBException
    {
        return listDBParameters;
    }

    public List<DB2Parameter> getDbmParameters(DBRProgressMonitor monitor) throws DBException
    {
        return listDBMParameters;
    }

    // -------------------------
    // Standards Getters
    // -------------------------

    public DBSObjectCache<DB2DataSource, DB2Schema> getSchemaCache()
    {
        return schemaCache;
    }

    public DBSObjectCache<DB2DataSource, DB2DataType> getDataTypeCache()
    {
        return dataTypeCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Tablespace> getTablespaceCache()
    {
        return tablespaceCache;
    }

}
