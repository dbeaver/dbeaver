/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.ext.oracle.model.plan.OraclePlanAnalyser;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericDataSource
 */
public class OracleDataSource extends JDBCDataSource implements DBSEntitySelector, DBCQueryPlanner, IAdaptable
{
    static final Log log = LogFactory.getLog(OracleDataSource.class);

    final SchemaCache schemaCache = new SchemaCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final TablespaceCache tablespaceCache = new TablespaceCache();

    private String activeSchemaName;
    private boolean isAdmin;

    public OracleDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
    }

    protected Map<String, String> getInternalConnectionProperties()
    {
        return OracleDataSourceProvider.getConnectionsProps();
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    @Association
    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return schemaCache.getObjects(monitor, this);
    }

    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException
    {
        return tablespaceCache.getObjects(monitor, this);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        dataTypeCache.getObjects(monitor, this);
        {
            final JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Load data source meta info");
            try {
                // Get user roles
                this.isAdmin = false;
                JDBCPreparedStatement dbStat = context.prepareStatement(
                    "SELECT GRANTED_ROLE FROM USER_ROLE_PRIVS");
                try {
                    JDBCResultSet resultSet = dbStat.executeQuery();
                    try {
                        while (resultSet.next()) {
                            final String role = resultSet.getString(1);
                            if (role.equals("DBA")) {
                                isAdmin = true;
                            }
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    dbStat.close();
                }

                // Get active schema
                dbStat = context.prepareStatement(
                    "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
                try {
                    JDBCResultSet resultSet = dbStat.executeQuery();
                    try {
                        resultSet.next();
                        activeSchemaName = resultSet.getString(1);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    dbStat.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
            finally {
                context.close();
            }
        }
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.schemaCache.clearCache();
        this.dataTypeCache.clearCache();
        this.activeSchemaName = null;

        this.initialize(monitor);

        return true;
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchemas(monitor);
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getSchema(monitor, childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleSchema.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    public boolean supportsEntitySelect()
    {
        return true;
    }

    public DBSEntity getSelectedEntity()
    {
        return schemaCache.getCachedObject(activeSchemaName);
    }

    public void selectEntity(DBRProgressMonitor monitor, DBSEntity entity)
        throws DBException
    {
        final DBSEntity oldSelectedEntity = getSelectedEntity();
        if (entity == oldSelectedEntity) {
            return;
        }
        if (!(entity instanceof OracleSchema)) {
            throw new IllegalArgumentException("Invalid object type: " + entity);
        }
        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Set active schema");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("ALTER SESSION SET CURRENT_SCHEMA=" + entity.getName());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
        activeSchemaName = entity.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeSchemaName != null) {
            DBUtils.fireObjectSelect(entity, true);
        }
    }

    public DBCPlan planQueryExecution(DBCExecutionContext context, String query) throws DBCException
    {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(this, query);
        plan.explain(context);
        return plan;
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new OracleStructureAssistant(this);
        }
        return null;
    }

    public OracleDataSource getDataSource() {
        return this;
    }

    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    class SchemaCache extends JDBCObjectCache<OracleDataSource, OracleSchema> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataSource owner) throws SQLException, DBException
        {
            StringBuilder schemasQuery = new StringBuilder("SELECT U.USERNAME,U.USER_ID FROM SYS.ALL_USERS u\n");
            List<String> schemaFilters = SQLUtils.splitFilter(getContainer().getSchemaFilter());
            schemasQuery.append("WHERE (EXISTS (SELECT 1 FROM ALL_OBJECTS WHERE OWNER=U.USERNAME)");
            final String curUserName = getContainer().getConnectionInfo().getUserName();
            if (!CommonUtils.isEmpty(curUserName)) {
                schemasQuery.append(" OR U.USERNAME='").append(curUserName.toUpperCase()).append("'");
            }
            schemasQuery.append(")");
            if (!schemaFilters.isEmpty()) {
                schemasQuery.append(" AND (");
                for (int i = 0; i < schemaFilters.size(); i++) {
                    if (i > 0) schemasQuery.append(" OR ");
                    schemasQuery.append("USERNAME LIKE ?");
                }
                schemasQuery.append(")");
            }
            //schemasQuery.append("\nUNION ALL SELECT 'PUBLIC' as USERNAME,1 as USER_ID FROM DUAL");
            schemasQuery.append("\nORDER BY USERNAME");
            JDBCPreparedStatement dbStat = context.prepareStatement(schemasQuery.toString());

            if (!schemaFilters.isEmpty()) {
                for (int i = 0; i < schemaFilters.size(); i++) {
                    dbStat.setString(i + 1, schemaFilters.get(i));
                }
            }
            return dbStat;
        }

        @Override
        protected OracleSchema fetchObject(JDBCExecutionContext context, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchema(OracleDataSource.this, resultSet);
        }
    }

    class DataTypeCache extends JDBCObjectCache<OracleDataSource, OracleDataType> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataSource owner) throws SQLException, DBException
        {
            return context.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPES WHERE OWNER IS NULL ORDER BY TYPE_NAME");
        }
        @Override
        protected OracleDataType fetchObject(JDBCExecutionContext context, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataType(OracleDataSource.this, resultSet);
        }
    }

    class TablespaceCache extends JDBCObjectCache<OracleDataSource, OracleTablespace> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataSource owner) throws SQLException, DBException
        {
            return context.prepareStatement(
                "SELECT * FROM SYS.DBA_TABLESPACES ORDER BY TABLESPACE_NAME");
        }
        @Override
        protected OracleTablespace fetchObject(JDBCExecutionContext context, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTablespace(OracleDataSource.this, resultSet);
        }
    }

}
