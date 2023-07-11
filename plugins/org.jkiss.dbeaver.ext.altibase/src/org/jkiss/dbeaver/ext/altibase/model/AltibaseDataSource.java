/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.plan.AltibaseExecutionPlan;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class AltibaseDataSource extends GenericDataSource implements DBCQueryPlanner {
    
    private static final Log log = Log.getLog(AltibaseDataSource.class);

    private GenericSchema publicSchema;
    private boolean isPasswordExpireWarningShown;
    private AltibaseOutputReader outputReader;
    
    public AltibaseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, AltibaseMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new AltibaseSQLDialect());
    }
    
    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        // PublicSchema is for global objects such as public synonym.
        publicSchema = new GenericSchema(this, null, AltibaseConstants.PUBLIC_USER);
        publicSchema.setVirtual(true);
    }
    
    @Override
    protected void initializeContextState(
            @NotNull DBRProgressMonitor monitor, 
            @NotNull JDBCExecutionContext context, 
            JDBCExecutionContext initFrom
            ) throws DBException {
        
        super.initializeContextState(monitor, context, initFrom);
        
        // Enable DBMS output
        if (outputReader == null) {
            outputReader = new AltibaseOutputReader();
        }
        
        outputReader.enableServerOutput(
            monitor,
            context,
            outputReader.isServerOutputEnabled());
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(outputReader);
        }
        return super.getAdapter(adapter);
    }
    
    @Override
    protected Connection openConnection(
            @NotNull DBRProgressMonitor monitor, 
            @Nullable JDBCExecutionContext context, 
            @NotNull String purpose
            ) throws DBCException {
        try {
            Connection connection = super.openConnection(monitor, context, purpose);
            try {
                for (SQLWarning warninig = connection.getWarnings();
                    warninig != null && !isPasswordExpireWarningShown;
                    warninig = warninig.getNextWarning()
                ) {
                    if (checkForPasswordWillExpireWarning(warninig)) {
                        isPasswordExpireWarningShown = true;
                    }
                }
            } catch (SQLException e) {
                log.debug("Can't get connection warnings", e);
            }
            return connection;
        } catch (DBCException e) {
            throw e;
        }
    }
    
    private boolean checkForPasswordWillExpireWarning(@NotNull SQLWarning warning) {
        if ((warning != null) && (warning.getErrorCode() == AltibaseConstants.EC_PASSWORD_WILL_EXPIRE)) {
            DBWorkbench.getPlatformUI().showWarningMessageBox(
                    AltibaseConstants.SQL_WARNING_TITILE,
                    warning.getMessage() + 
                    AltibaseConstants.NEW_LINE + 
                    AltibaseConstants.PASSWORD_WILL_EXPIRE_WARN_DESCRIPTION
                    );
            return true;
        }
        return false;
    }
        
    @NotNull
    @Override
    public AltibaseDataSource getDataSource() {
        return this;
    }

    @Override
    public boolean splitProceduresAndFunctions() {
        return true;
    }
    
    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return AltibaseTable.class;
    }

    public Collection<? extends GenericSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getSynonyms(monitor);
    }

    ///////////////////////////////////////////////
    // Plan
    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query, 
            @NotNull DBCQueryPlannerConfiguration configuration) throws DBException {
        AltibaseExecutionPlan plan = new AltibaseExecutionPlan(this, (JDBCSession) session, query);
        plan.explain();
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }
    
    ///////////////////////////////////////////////
    // DBMS Procedure Output
    private class AltibaseOutputReader implements DBCServerOutputReader {
        
        private StringBuilder mSb4CallBackMsg = new StringBuilder();
        
        @Override
        public boolean isServerOutputEnabled() {
            return getContainer().getPreferenceStore().getBoolean(AltibaseConstants.PREF_DBMS_OUTPUT);
        }

        @Override
        public boolean isAsyncOutputReadSupported() {
            return false;
        }

        public void enableServerOutput(
                DBRProgressMonitor monitor, 
                DBCExecutionContext context, 
                boolean enable
                ) throws DBCException {
            
            Connection conn = null;
            ClassLoader classLoader = null;
            @SuppressWarnings("rawtypes")
            Class class4MsgCallback = null;
            Object instance4Callback = null;
            Method method2RegisterCallback = null;
            
            try (JDBCSession session = (JDBCSession) context.openSession(monitor, 
                    DBCExecutionPurpose.UTIL, (enable ? "Enable" : "Disable") + " DBMS output")) {
                
                conn = session.getOriginal();
                classLoader = conn.getClass().getClassLoader();
                if (classLoader == null) {
                    throw new SecurityException("Failed to load ClassLoader");
                }
                
                class4MsgCallback = classLoader.loadClass(AltibaseConstants.CLASS_NAME_4_MESSAGE_CALLBACK);
                if (class4MsgCallback == null) {
                    throw new ClassNotFoundException(
                            "Failed to load class: " + AltibaseConstants.CLASS_NAME_4_MESSAGE_CALLBACK);
                }

                instance4Callback = Proxy.newProxyInstance(classLoader, new java.lang.Class[]
                        { class4MsgCallback }, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("print".equals(method.getName())) {
                            mSb4CallBackMsg.append((String) args[0]);
                        }

                        return null;
                    }
                });
                if (instance4Callback == null) {
                    throw new NullPointerException(
                            "Failed to instantiate class: " + AltibaseConstants.CLASS_NAME_4_MESSAGE_CALLBACK);
                }

                method2RegisterCallback = classLoader
                        .loadClass(AltibaseConstants.CLASS_NAME_4_CONNECTION)
                        .getMethod(
                            AltibaseConstants.METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK, 
                            class4MsgCallback);
                
                if (method2RegisterCallback == null) {
                    throw new NoSuchMethodException(String.format(
                            "Failed to get method: %s of class %s ", 
                            AltibaseConstants.METHOD_NAME_4_REGISTER_MESSAGE_CALLBACK,
                            AltibaseConstants.CLASS_NAME_4_MESSAGE_CALLBACK));
                }
                
                method2RegisterCallback.invoke(conn, instance4Callback);
                
            } catch (Exception e) {
                log.error("Failed to register DBMS output message callback method: " + e.getMessage());
                throw new DBCException(e, context);
            }
        }

        @Override
        public void readServerOutput(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBCExecutionContext context,
                @Nullable DBCExecutionResult executionResult,
                @Nullable DBCStatement statement,
                @NotNull DBCOutputWriter output
                ) throws DBCException {
            if (mSb4CallBackMsg != null) {
                output.println(null, mSb4CallBackMsg.toString());
                mSb4CallBackMsg.delete(0, mSb4CallBackMsg.length());
            }
        }
    }
}
