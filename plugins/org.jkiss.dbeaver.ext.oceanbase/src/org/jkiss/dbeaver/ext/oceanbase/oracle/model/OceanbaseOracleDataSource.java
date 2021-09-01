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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oceanbase.data.OceanbaseValueHandlerProvider;
import org.jkiss.dbeaver.ext.oceanbase.model.plan.OceanbasePlanAnalyzer;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleGrantee;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUser;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

public class OceanbaseOracleDataSource extends OracleDataSource{
	private final String tenantType;
    private OracleSchema publicSchema;
    final OceanbaseUserCache oceanbaseUserCache = new OceanbaseUserCache();
    final public OceanbaseSchemaCache oceanbaseSchemaCache = new OceanbaseSchemaCache();



	public OceanbaseOracleDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
		super(monitor, container);
		this.tenantType = container.getActualConnectionConfiguration().getProviderProperty("tenantType");
	}
	
	@Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.publicSchema = new OracleSchema(this, 1, OracleConstants.USER_PUBLIC);
		super.initialize(monitor);
	}
	
	public boolean isMySQLMode () {
    	return tenantType.equals("MySQL");
    }
	
	@Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBDValueHandlerProvider.class) {
        	return adapter.cast(new OceanbaseValueHandlerProvider());
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new OceanbasePlanAnalyzer(this));
        } else {
        	return super.getAdapter(adapter);
        }
    }
	
	static class OceanbaseSchemaCache extends JDBCObjectCache<OceanbaseOracleDataSource, OceanbaseOracleSchema> {
        OceanbaseSchemaCache() {
            setListOrderComparator(DBUtils.<OceanbaseOracleSchema>nameComparator());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OceanbaseOracleDataSource owner) throws SQLException {
            StringBuilder schemasQuery = new StringBuilder();
            // PROP_CHECK_SCHEMA_CONTENT set to true when option "Hide empty schemas" is set
            boolean showAllSchemas = ! CommonUtils.toBoolean(owner.getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_CHECK_SCHEMA_CONTENT));
            schemasQuery.append("SELECT U.* FROM ").append(OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "USERS")).append(" U\n");

            schemasQuery.append(
                "WHERE (");
            if (showAllSchemas) {
                schemasQuery.append("U.USERNAME IS NOT NULL");
            } else {
                schemasQuery.append("U.USERNAME IN (SELECT DISTINCT OWNER FROM ").append(OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "OBJECTS")).append(")");
            }

            DBSObjectFilter schemaFilters = owner.getContainer().getObjectFilter(OceanbaseOracleSchema.class, null, false);
            if (schemaFilters != null) {
                JDBCUtils.appendFilterClause(schemasQuery, schemaFilters, "U.USERNAME", false);
            }
            schemasQuery.append(")");

            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());

            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }
            return dbStat;
        }

        @Override
        protected OceanbaseOracleSchema fetchObject(@NotNull JDBCSession session, @NotNull OceanbaseOracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OceanbaseOracleSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OceanbaseOracleDataSource owner, Iterator<OceanbaseOracleSchema> objectIter) {
            setListOrderComparator(DBUtils.<OceanbaseOracleSchema>nameComparator());
        }
    }
	
	static class OceanbaseUserCache extends JDBCObjectCache<OceanbaseOracleDataSource, OceanbaseOracleUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OceanbaseOracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "USERS") + " ORDER BY USERNAME");
        }

        @Override
        protected OceanbaseOracleUser fetchObject(@NotNull JDBCSession session, @NotNull OceanbaseOracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OceanbaseOracleUser(owner, resultSet);
        }
    }
	
	@Association
	@Override
    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(oceanbaseSchemaCache.getAllObjects(monitor, this));
    }

	@Override
    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
            return publicSchema;
        }
        // Schema cache may be null during DataSource initialization
        return oceanbaseSchemaCache == null ? null : oceanbaseSchemaCache.getObject(monitor, this, name);
    }
	
	@Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);
        this.oceanbaseSchemaCache.clearCache();
        this.oceanbaseUserCache.clearCache();
        this.initialize(monitor);

        return this;
    }
	
	@Association
	@Override
    public Collection<OracleUser> getUsers(DBRProgressMonitor monitor) throws DBException {
		return new ArrayList<>(oceanbaseUserCache.getAllObjects(monitor, this));
    }
	
	@Association
	@Override
    public OracleUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return oceanbaseUserCache.getObject(monitor, this, name);
    }
	
	@Override
	public OracleGrantee getGrantee(DBRProgressMonitor monitor, String name) throws DBException {
        OracleUser user = oceanbaseUserCache.getObject(monitor, this, name);
        if (user != null) {
            return user;
        }
        return super.getGrantee(monitor, name);
    }
	
}
