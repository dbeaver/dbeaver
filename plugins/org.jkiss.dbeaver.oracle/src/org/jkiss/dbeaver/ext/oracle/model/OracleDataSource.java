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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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

    private List<OracleEngine> engines;
    private List<OracleSchema> schemas;
    private List<OraclePrivilege> privileges;
    private List<OracleUser> users;
    private List<OracleCharset> charsets;
    private Map<String, OracleCollation> collations;
    private String activeSchemaName;
    //private List<OracleInformationFolder> informationFolders;

    public OracleDataSource(DBSDataSourceContainer container)
        throws DBException
    {
        super(container);
    }

    protected Properties getInternalConnectionProperties()
    {
        return OracleDataSourceProvider.getConnectionsProps();
    }

    public String[] getTableTypes()
    {
        return OracleConstants.TABLE_TYPES;
    }

    public List<OracleSchema> getSchemas()
    {
        return schemas;
    }

    public OracleSchema getSchema(String name)
    {
        return DBUtils.findObject(schemas, name);
    }

    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Load basic oracle metadata");
        try {
            // Read engines
            {
                engines = new ArrayList<OracleEngine>();
                JDBCPreparedStatement dbStat = context.prepareStatement("SHOW ENGINES");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            OracleEngine engine = new OracleEngine(this, dbResult);
                            engines.add(engine);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
            }

            // Read charsets and collations
            {
                charsets = new ArrayList<OracleCharset>();
                JDBCPreparedStatement dbStat = context.prepareStatement("SHOW CHARSET");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            OracleCharset charset = new OracleCharset(this, dbResult);
                            charsets.add(charset);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
                Collections.sort(charsets, DBUtils.<OracleCharset>nameComparator());


                collations = new LinkedHashMap<String, OracleCollation>();
                dbStat = context.prepareStatement("SHOW COLLATION");
                try {
                    JDBCResultSet dbResult = dbStat.executeQuery();
                    try {
                        while (dbResult.next()) {
                            String charsetName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CHARSET);
                            OracleCharset charset = getCharset(charsetName);
                            if (charset == null) {
                                log.warn("Charset '" + charsetName + "' not found.");
                                continue;
                            }
                            OracleCollation collation = new OracleCollation(charset, dbResult);
                            collations.put(collation.getName(), collation);
                            charset.addCollation(collation);
                        }
                    } finally {
                        dbResult.close();
                    }
                } catch (SQLException ex ) {
                    // Engines are not supported. Shame on it. Leave this list empty
                } finally {
                    dbStat.close();
                }
            }

            {
                // Read schemas
                List<OracleSchema> tmpSchemas = new ArrayList<OracleSchema>();
                StringBuilder schemasQuery = new StringBuilder("SELECT * FROM " + OracleConstants.META_TABLE_SCHEMATA);
                List<String> schemaFilters = SQLUtils.splitFilter(getContainer().getSchemaFilter());
                if (!schemaFilters.isEmpty()) {
                    schemasQuery.append(" WHERE ");
                    for (int i = 0; i < schemaFilters.size(); i++) {
                        if (i > 0) schemasQuery.append(" OR ");
                        schemasQuery.append(OracleConstants.COL_SCHEMA_NAME).append(" LIKE ?");
                    }
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
                this.schemas = tmpSchemas;
            }

            {
                // Get active schema
                try {
                    JDBCPreparedStatement dbStat = context.prepareStatement("SELECT DATABASE()");
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
            throw new DBException("Error reading metadata", ex);
        }
        finally {
            context.close();
        }

/*
        // Construct information folders
        informationFolders = new ArrayList<OracleInformationFolder>();
        informationFolders.add(new OracleInformationFolder(this, "Session Status") {
            public Collection getObjects(DBRProgressMonitor monitor) throws DBException
            {
                return getSessionStatus(monitor);
            }
        });
        informationFolders.add(new OracleInformationFolder(this, "Global Status") {
            public Collection getObjects(DBRProgressMonitor monitor) throws DBException
            {
                return getGlobalStatus(monitor);
            }
        });
        informationFolders.add(new OracleInformationFolder(this, "Session Variables") {
            public Collection getObjects(DBRProgressMonitor monitor) throws DBException
            {
                return getSessionVariables(monitor);
            }
        });
        informationFolders.add(new OracleInformationFolder(this, "Global Variables") {
            public Collection getObjects(DBRProgressMonitor monitor) throws DBException
            {
                return getGlobalVariables(monitor);
            }
        });
        informationFolders.add(new OracleInformationFolder(this, "Engines") {
            public Collection getObjects(DBRProgressMonitor monitor)
            {
                return getEngines();
            }
        });
        informationFolders.add(new OracleInformationFolder(this, "User Privileges") {
            public Collection getObjects(DBRProgressMonitor monitor) throws DBException
            {
                return getPrivileges(monitor);
            }
        });
*/
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);

        this.engines = null;
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
        OracleSchema schema = getSchema(schemaName);
        if (schema == null) {
            log.error("Schema " + schemaName + " not found");
            return null;
        }
        return schema.getTable(monitor, tableName);
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchemas();
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getSchema(childName);
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
        return getSchema(activeSchemaName);
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
            JDBCPreparedStatement dbStat = context.prepareStatement("use " + entity.getName());
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

    public List<OracleEngine> getEngines()
    {
        return engines;
    }

    public OracleEngine getEngine(String name)
    {
        return DBUtils.findObject(engines, name);
    }

    public OracleEngine getDefaultEngine()
    {
        for (OracleEngine engine : engines) {
            if (engine.getSupport() == OracleEngine.Support.DEFAULT) {
                return engine;
            }
        }
        return null;
    }

    public Collection<OracleCharset> getCharsets()
    {
        return charsets;
    }

    public OracleCharset getCharset(String name)
    {
        for (OracleCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

    public OracleCollation getCollation(String name)
    {
        return collations.get(name);
    }

    public List<OraclePrivilege> getPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        if (privileges == null) {
            privileges = loadPrivileges(monitor);
        }
        return privileges;
    }

    public List<OraclePrivilege> getPrivilegesByKind(DBRProgressMonitor monitor, OraclePrivilege.Kind kind)
        throws DBException
    {
        List<OraclePrivilege> privs = new ArrayList<OraclePrivilege>();
        for (OraclePrivilege priv : getPrivileges(monitor)) {
            if (priv.getKind() == kind) {
                privs.add(priv);
            }
        }
        return privs;
    }

    public OraclePrivilege getPrivilege(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPrivileges(monitor), name);
    }

    private List<OraclePrivilege> loadPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load privileges");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("SHOW PRIVILEGES");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<OraclePrivilege> privileges = new ArrayList<OraclePrivilege>();
                    while (dbResult.next()) {
                            OraclePrivilege user = new OraclePrivilege(this, dbResult);
                            privileges.add(user);
                        }
                    return privileges;
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

    public List<OracleParameter> getSessionStatus(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, true, false);
    }

    public List<OracleParameter> getGlobalStatus(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, true, true);
    }

    public List<OracleParameter> getSessionVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, false);
    }

    public List<OracleParameter> getGlobalVariables(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadParameters(monitor, false, true);
    }

    public List<OracleDataSource> getInformation()
    {
        return Collections.singletonList(this);
    }

    private List<OracleParameter> loadParameters(DBRProgressMonitor monitor, boolean status, boolean global) throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load status");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SHOW " + 
                (global ? "GLOBAL " : "") + 
                (status ? "STATUS" : "VARIABLES"));
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    List<OracleParameter> parameters = new ArrayList<OracleParameter>();
                    while (dbResult.next()) {
                        OracleParameter parameter = new OracleParameter(
                            this,
                            JDBCUtils.safeGetString(dbResult, "variable_name"),
                            JDBCUtils.safeGetString(dbResult, "value"));
                        parameters.add(parameter);
                    }
                    return parameters;
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
