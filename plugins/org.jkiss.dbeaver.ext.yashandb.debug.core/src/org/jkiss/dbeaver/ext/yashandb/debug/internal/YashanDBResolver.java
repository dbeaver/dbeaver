
package org.jkiss.dbeaver.ext.yashandb.debug.internal;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.debug.core.YashanDBDebugCore;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class YashanDBResolver implements DBGResolver {

    private final DBPDataSourceContainer dataSource;

    public YashanDBResolver(DBPDataSourceContainer dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObject resolveObject(Map<String, Object> context, Object identifier, DBRProgressMonitor monitor)
            throws DBException {
        //TODO: yangmeng支持oid和name都行, 暂时支持name
        //Collection<YashanDBProcedureStandalone> procedureStandalones = ((YashanDBDataSource) dataSource.getDataSource())
        //        .getSchema(monitor, context.get(YashanDBDebugConstants.ATTR_SCHEMA_NAME).toString()).getProcedures(monitor);

        // 跨schema场景下这样不行, 必须获取当前datasource下所有的PLSQL
        List<YashanDBProcedureStandalone> collect = ((YashanDBDataSource) dataSource.getDataSource())
                .getSchemas(monitor).stream().flatMap(t -> {
                    try {
                        return t.getProcedures(monitor).stream();
                    } catch (DBException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        String functionName =null;
        String functionSchema =null;
        for (YashanDBProcedureStandalone a : collect) {
            if (a.getObjectId() == Long.parseLong(identifier.toString())){
                functionName= a.getName();
                functionSchema=a.getSchema().getName();
                break;
            }
        }

        //String name = procedureStandalones.stream().filter(t -> t.getObjectId()==Long.parseLong(identifier.toString())).collect(Collectors.toList()).get(0).getName();
        return YashanDBDebugCore.resolveFunction(monitor, dataSource, context, functionName, functionSchema);
    }

    @Override
    public Map<String, Object> resolveContext(DBSObject databaseObject) {
        HashMap<String, Object> context = new HashMap<>();
        if (databaseObject instanceof YashanDBProcedureStandalone) {
            YashanDBDebugCore.saveFunction((YashanDBProcedureStandalone)databaseObject, context);
        }
        return context;
    }

}
