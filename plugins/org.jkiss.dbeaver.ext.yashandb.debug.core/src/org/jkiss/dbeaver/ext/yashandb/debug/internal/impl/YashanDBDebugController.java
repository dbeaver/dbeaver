
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBMarkers;
import org.jkiss.utils.CommonUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class YashanDBDebugController extends DBGBaseController {

    private static final Log log = Log.getLog(YashanDBDebugController.class);

    private  YashanDBDebugSession yashanDBDebugSession = null;

    public YashanDBDebugController(DBPDataSourceContainer dataSourceContainer, Map<String, Object> configuration) {
        super(dataSourceContainer, configuration);
    }

    @Override
    public YashanDBDebugSession createSession(DBRProgressMonitor monitor, Map<String, Object> configuration)
            throws DBGException {
        YashanDBDebugSession yashanDBSession = new YashanDBDebugSession(monitor, this);

        try {
            yashanDBSession.startYashanDBDebug(monitor, configuration);
        }catch (Exception e){
            log.error(e);
            throw new DBGException(e.getMessage());
        }

        yashanDBDebugSession = yashanDBSession;
        log.debug("Creating yashandb debug session");
        return yashanDBSession;

    }

    //只识别ys的的这个OID的断点即可
    @Override
    public DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes) throws DBGException {
        //attributes 是工作空间的断点相关信息, debugConfiguration才是正在当前的PLSQL
        Map<String, Object> debugConfiguration = getDebugConfiguration();
        String currDatasourceId = attributes.get(DBMarkers.MARKER_ATTRIBUTE_DATASOURCE_ID).toString();
        if (!currDatasourceId.equals(debugConfiguration.get(DBGConstants.ATTR_DATASOURCE_ID).toString()))
            return null;

        //判断一个场景, 过期的断点在这里校验, 提示
        long oid = Long.parseLong(debugConfiguration.get(YashanDBDebugConstants.attrFunctionOid).toString());
        long pointOid = Long.parseLong(attributes.get(YashanDBDebugConstants.attrFunctionOid).toString());
        String pointName = attributes.get(YashanDBDebugConstants.ATTR_FUNCTION_NAME).toString();
        String pointLine = attributes.get(YashanDBDebugConstants.LINE_NUMBER).toString();
        if (yashanDBDebugSession.getProcedures().stream().noneMatch(t -> t.getObjectId()==pointOid))
            throw new DBGException(String.format("name: %s, line: %s have expired, please reset the breakpoint", pointName, pointLine));

        YashanDBProcedureStandalone yashanDBProcedureStandalone = yashanDBDebugSession.getProcedures().stream()
                .filter(t -> t.getObjectId() == oid)
                .collect(Collectors.toList()).get(0);

        //long oid = CommonUtils.toLong(attributes.get(YashanDBDebugConstants.ATTR_FUNCTION_OID));
        //String oname=CommonUtils.toString(attributes.get(YashanDBDebugConstants.ATTR_FUNCTION_NAME));
        //int parsed = CommonUtils.toInt(attributes.get(IMarker.LINE_NUMBER));

        return YashanDBDebugBreakpointDescriptor.fromMap(attributes);
    }

}
