/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.hana.model.plan.HANAPlanAnalyser;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class HANADataSource extends GenericDataSource implements DBCQueryPlanner {

    private static final Log log = Log.getLog(HANADataSource.class);

    private HashMap<String, String> sysViewColumnUnits; 
    private boolean isPasswordExpireWarningShown;
    
    public HANADataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new HANASQLDialect());
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
    {
        final HANADataSourceInfo info = new HANADataSourceInfo(metaData);
        return info;
    }
    
    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        if (HANAConstants.DATA_TYPE_NAME_REAL_VECTOR.equalsIgnoreCase(typeName)) {
            return DBPDataKind.ARRAY;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    /*
     * search
     */
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new HANAStructureAssistant(this));
        } else if (adapter == DBAUserPasswordManager.class) {
            return adapter.cast(new HANAUserPasswordManager(this));
        }
        return super.getAdapter(adapter);
    }
    
    /*
     * explain
     */
    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query, @NotNull DBCQueryPlannerConfiguration configuration)
    throws DBCException {
        HANAPlanAnalyser plan = new HANAPlanAnalyser(this, query);
        plan.explain(session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }
  
    /*
     * application
     */
    @Override
    protected boolean isPopulateClientAppName() { 
        return false; // basically true, but different property name 
    } 

    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = new HashMap<>();
        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            String appName = DBUtils.getClientApplicationName(getContainer(), context, purpose);
            props.put(HANAConstants.CONN_PROP_APPLICATION_NAME, appName);
        }
        if (getContainer().isConnectionReadOnly()) {
            props.put(HANAConstants.CONN_PROP_READONLY, "TRUE");
        }
        // Represent geometries as EWKB (instead of as WKB) so that we can extract the SRID
        props.put(HANAConstants.CONN_PROP_SPATIAL_OUTPUT_REPRESENTATION, HANAConstants.CONN_VALUE_SPATIAL_OUTPUT_REPRESENTATION);
        // Represent empty points using NaN-coordinates
        props.put(HANAConstants.CONN_PROP_SPATIAL_WKB_EMPTY_POINT_REPRESENTATION, HANAConstants.CONN_VALUE_SPATIAL_WKB_EMPTY_POINT_REPRESENTATION);
        return props;
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context,
                                        @NotNull String purpose) throws DBCException {
        Connection connection = super.openConnection(monitor, context, purpose);
        try {
            Statement statement = connection.createStatement();
            statement.execute("SELECT * FROM SYS.DUMMY");
        } catch (SQLException e) {
            if (e.getErrorCode() == HANAConstants.ERR_SQL_ALTER_PASSWORD_NEEDED) {
                if (changeExpiredPassword(monitor, context, purpose)) {
                    return openConnection(monitor, context, purpose);
                }
            } else {
                log.debug("password expired check failed ", e);
            }
        }

        try {
            for (SQLWarning warning = connection.getWarnings(); warning != null; warning = warning.getNextWarning()) {
                if (warning.getErrorCode() == HANAConstants.WRN_SQL_NEARLY_EXPIRED_PASSWORD && !isPasswordExpireWarningShown) {
                    isPasswordExpireWarningShown = true;
                    DBWorkbench.getPlatformUI().showWarningMessageBox("Warning", warning.getMessage());
                }
            }
        } catch (SQLException e) {
            log.debug("password expire check failed", e);
        }
        return connection;
    }    

    private boolean changeExpiredPassword(DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose) {
        DBPConnectionConfiguration connectionInfo = getContainer().getActualConnectionConfiguration();
        DBAPasswordChangeInfo passwordInfo = DBWorkbench.getPlatformUI().promptUserPasswordChange(
                "Password has expired. Set new password.", connectionInfo.getUserName(), connectionInfo.getUserPassword(), false, false);
        if (passwordInfo == null) {
            return false;
        }
        try {
            if (passwordInfo.getNewPassword() == null) {
                throw new DBException("You can't set empty password");
            }
            Connection connection = super.openConnection(monitor, context, purpose);
            Statement statement = connection.createStatement();
            statement.execute("ALTER USER " + connectionInfo.getUserName() + " PASSWORD " + DBUtils.getQuotedIdentifier(this, passwordInfo.getNewPassword()));
            
            connectionInfo.setUserPassword(passwordInfo.getNewPassword());
            getContainer().getConnectionConfiguration().setUserPassword(passwordInfo.getNewPassword());
            getContainer().persistConfiguration();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Error changing password", "Error changing expired password", e);
            return false;
        }
        return true;
    }

    /*
     * column unit for views in SYS schema
     */
    public void initializeSysViewColumnUnits(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sysViewColumnUnits != null)
            return;
        sysViewColumnUnits = new HashMap<String, String>();
        String stmt = "SELECT VIEW_NAME||'.'||VIEW_COLUMN_NAME, UNIT FROM SYS.M_MONITOR_COLUMNS WHERE UNIT IS NOT NULL";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read generic metadata")) {
            try {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
                    try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                        while(resultSet.next()) {
                            sysViewColumnUnits.put(resultSet.getString(1), resultSet.getString(2));
                        }
                    }
                }
            } catch (SQLException e) {
                log.debug("Error getting SYS column units: " + e.getMessage());
            }
        }
    }
    
    String getSysViewColumnUnit(String objectName, String columnName)
    {
        return sysViewColumnUnits.get(objectName+"."+columnName);
    }
}
