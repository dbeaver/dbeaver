
package org.jkiss.dbeaver.ext.yashandb.debug.core;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class YashanDBDebugCore {
	public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.ext.yashandb.debug.core"; //$NON-NLS-1$

    public static void saveFunction(YashanDBProcedureStandalone procedure, Map<String, Object> configuration) {
        YashanDBDataSource dataSource = procedure.getDataSource();
        DBPDataSourceContainer dataSourceContainer = dataSource.getContainer();

        String schemaName = procedure.getSchema().getName();

        configuration.put(DBGConstants.ATTR_PROJECT_NAME, dataSourceContainer.getProject().getName());
        configuration.put(DBGConstants.ATTR_DATASOURCE_ID, dataSourceContainer.getId());
        configuration.put(DBGConstants.ATTR_DEBUG_TYPE, YashanDBDebugConstants.DEBUG_TYPE_FUNCTION);
        configuration.put(YashanDBDebugConstants.ATTR_SCHEMA_NAME, schemaName);
        configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_OID, String.valueOf(procedure.getObjectId()));
        configuration.put(YashanDBDebugConstants.ATTR_FUNCTION_NAME,String.valueOf(procedure.getName()));
    }

    public static YashanDBProcedureStandalone resolveFunction(DBRProgressMonitor monitor, DBPDataSourceContainer dsContainer,
                                                              Map<String, Object> configuration, String nextFunctionName,
                                                              String nextFunctionSchema) throws DBException {
        if (!dsContainer.isConnected()) {
            dsContainer.connect(monitor, true, true);
        }
        //YM: 这是母体, 需要的是嵌套的子函数, 只有堆栈才知道
        String functionName = nextFunctionName ==null ? CommonUtils.toString(configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_NAME)): nextFunctionName;
        String schemaName = nextFunctionSchema ==null ? (String)configuration.get(YashanDBDebugConstants.ATTR_SCHEMA_NAME): nextFunctionSchema;
        YashanDBDataSource ds = (YashanDBDataSource) dsContainer.getDataSource();

        YashanDBSchema schema = ds.getSchema(monitor, schemaName);
        if (schema != null) {
            //force refresh args
            schema.getProcedure(monitor, functionName).refreshObject(monitor);
            YashanDBProcedureStandalone function = schema.getProcedure(monitor, functionName);
            if (function != null) {
                return function;
            }
            throw new DBException("YashanDB Function " + functionName + " not found in schema " + schemaName);
        } else {
            throw new DBException("YashanDB Schema '" + schemaName + "' not found ");
        }
    }

}
