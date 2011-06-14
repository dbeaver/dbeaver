/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

import java.sql.SQLException;
import java.util.*;

/**
 * GenericDataSource
 */
public class OracleDataSource extends JDBCDataSource implements DBSEntitySelector, DBCQueryPlanner, IAdaptable
{
    static final Log log = LogFactory.getLog(OracleDataSource.class);

    private Map<String, OracleSchema> schemas;
    private List<OracleUser> users;
    private String activeSchemaName;

    public OracleDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
    }

    protected Map<String, String> getInternalConnectionProperties()
    {
        return OracleDataSourceProvider.getConnectionsProps();
    }

    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        if (schemas == null) {
            loadSchemas(monitor);
        }
        return schemas.values();
    }

    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        if (schemas == null) {
            loadSchemas(monitor);
        }
        return schemas.get(name);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);
    }

    private void loadSchemas(DBRProgressMonitor monitor) throws DBException
    {
        List<OracleSchema> tmpSchemas = new ArrayList<OracleSchema>();

        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Load basic oracle metadata");
        try {
            {
                // Read schemas
                // Read only non-empty schemas and current user's schema
                StringBuilder schemasQuery = new StringBuilder("SELECT * FROM SYS.ALL_USERS u\n");
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
                JDBCPreparedStatement dbStat = context.prepareStatement(schemasQuery.toString());
                try {
                    if (!schemaFilters.isEmpty()) {
                        for (int i = 0; i < schemaFilters.size(); i++) {
                            dbStat.setString(i + 1, schemaFilters.get(i));
                        }
                    }
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            OracleSchema schema = new OracleSchema(this, dbResult);
                            if (!getContainer().isShowSystemObjects() && schema.getName().equalsIgnoreCase(OracleConstants.INFO_SCHEMA_NAME)) {
                                // Skip system schemas
                                continue;
                            }
                            tmpSchemas.add(schema);
                        }
                    } finally {
                        dbResult.close();
                    }
                }
                finally {
                    dbStat.close();
                }
            }

            {
                // Get active schema
                try {
                    JDBCPreparedStatement dbStat = context.prepareStatement("SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
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
            }

        } catch (SQLException ex) {
            throw new DBException("Error reading schemas", ex);
        }
        finally {
            context.close();
        }

        Collections.sort(tmpSchemas, DBUtils.<OracleSchema>nameComparator());

        // Make map
        Map<String, OracleSchema> schemas = new LinkedHashMap<String, OracleSchema>(tmpSchemas.size());
        for (OracleSchema schema : tmpSchemas) {
            schemas.put(schema.getName(), schema);
        }
        this.schemas = schemas;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.schemas = null;
        this.users = null;
        this.activeSchemaName = null;

        this.initialize(monitor);

        return true;
    }

    OracleTable findTable(DBRProgressMonitor monitor, String schemaName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(schemaName)) {
            return null;
        }
        OracleSchema schema = getSchema(monitor, schemaName);
        if (schema == null) {
            log.error("Schema " + schemaName + " not found");
            return null;
        }
        return schema.getTable(monitor, tableName);
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
        if (schemas == null) {
            return null;
        }
        return schemas.get(activeSchemaName);
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

    public List<OracleUser> getUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    public OracleUser getUser(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getUsers(monitor), name);
    }

    private List<OracleUser> loadUsers(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load users");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("SELECT * FROM oracle.user ORDER BY user");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<OracleUser> userList = new ArrayList<OracleUser>();
                    while (dbResult.next()) {
                            OracleUser user = new OracleUser(this, dbResult);
                            userList.add(user);
                        }
                    return userList;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit();
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll();
        }
        return super.createQueryTransformer(type);
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
}
