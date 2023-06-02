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

import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.plan.AltibaseExecutionPlan;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class AltibaseDataSource extends GenericDataSource implements DBCQueryPlanner {

    private static final Log log = Log.getLog(AltibaseDataSource.class);
    
    private GenericSchema publicSchema;
    
    public AltibaseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, AltibaseMetaModel metaModel)
        throws DBException {
        super(monitor, container, metaModel, new AltibaseSQLDialect());
        
    }
 
    /* TODO: DB33. Procedure PRINTLN */
    // private String mCallbackMsg = null;
	/**
	 * BUG-45342 Need to display the output from PSM execution using JDBC callback.
	 * 
	 * @see https://altra.altibase.com/altimis-2.0/app_bug_new/bug_view.jsp?pk=45342
	 * @see https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
	 */
    /*
    @SuppressWarnings("rawtypes")
    protected void setCallBack(boolean aPrintConsole) {
        Class class4CallbackInterface = null;
        Object instance4Callback = null;
         
        Method method4RegisterCallback = null;
        ClassLoader sClassLoader = java.sql.DriverManager.class.getClassLoader();
         
        try {
            class4CallbackInterface = sClassLoader
                    .loadClass("Altibase.jdbc.driver.AltibaseMessageCallback");
            instance4Callback = getCallBack(aPrintConsole); // BUG-45839
 
            method4RegisterCallback = sClassLoader.loadClass(
                    "Altibase.jdbc.driver.AltibaseConnection")
                    .getDeclaredMethod("registerMessageCallback",
                            class4CallbackInterface);
            method4RegisterCallback.invoke(mConn, instance4Callback);
        } catch (Throwable t)
        {
            log.warn("Fail to register PSM message callback.");
        }
    }
    
    public String getCallBackMessage() { return mCallbackMsg.toString(); }
    */
	/**
	 * @see BUG-45839 Need to support shard table data rebuild.
	 * 		https://altra.altibase.com/altimis-2.0/app_bug_new/bug_view.jsp?pk=45839
	 */
	/*
	private Object getCallBack(final boolean aPrintConsole) throws ClassNotFoundException
	{
		ClassLoader sClassLoader = java.sql.DriverManager.class.getClassLoader();
		@SuppressWarnings("rawtypes")
		Class class4CallbackInterface = sClassLoader
				.loadClass(CLASS_NAME_4_CALLBACK_INTERFACE);

		if (!aPrintConsole)
			mSb4CallBackMsg = new StringBuilder();

		return Proxy.newProxyInstance(mUrlClassLoader, new java.lang.Class[]
			{ class4CallbackInterface }, new InvocationHandler()
		{
			public Object invoke(Object aProxy, Method aMethod, Object[] aArgs)
					throws Throwable
			{
				String methodName = aMethod.getName();

				//
				// Implementation of the 'print' method in the
				// 'AltibaseMessageCallback' interface
				//
				if (aPrintConsole)
				{
					if (methodName.equals("print"))
						System.out.println(mDbQueryExecutor
								.prefixDbName((String) aArgs[0]));
				}
				else
				{
					if (methodName.equals("print"))
						mSb4CallBackMsg.append((String) aArgs[0]);
				}
				
				return null;
			}
		});
	}
	*/
    
    /* FIXME: parameter doesn't work */
    public boolean splitProceduresAndFunctions() {
    	return true;
    }
    
    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        
        // PublicSchema is for global objects such as public synonym.
        publicSchema = new GenericSchema(this, null, AltibaseConstants.PUBLIC_USER);
        publicSchema.setVirtual(true);
    }

    @NotNull
    @Override
    public AltibaseDataSource getDataSource() {
        return this;
    }

    /*
    @Override
    public List<AltibaseTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        return (List<AltibaseTable>) super.getPhysicalTables(monitor);
    }

    @Override
    public List<AltibaseTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return (List<AltibaseTable>) super.getTables(monitor);
    }

    @Override
    public List<AltibaseProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return (List<AltibaseProcedure>) super.getProcedures(monitor);
    }
    
    @Override
    public List<AltibaseTableTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        return (List<AltibaseTableTrigger>) super.getTableTriggers(monitor);
    }
	*/
    
    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return AltibaseTable.class;
    }
    
    public Collection<AltibaseSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return (Collection<AltibaseSynonym>) publicSchema.getSynonyms(monitor);
    }
    
    ///////////////////////////////////////////////
    // Plan
	@NotNull
    @Override
	public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query, @NotNull DBCQueryPlannerConfiguration configuration) throws DBException {
		AltibaseExecutionPlan plan = new AltibaseExecutionPlan(this, (JDBCSession) session, query);
        plan.explain();
        return plan;
	}

	@NotNull
    @Override
	public DBCPlanStyle getPlanStyle() {
		return DBCPlanStyle.PLAN;
	}
}
