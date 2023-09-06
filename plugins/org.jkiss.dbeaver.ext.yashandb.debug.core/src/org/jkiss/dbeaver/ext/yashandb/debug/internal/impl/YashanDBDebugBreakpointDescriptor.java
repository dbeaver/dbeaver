
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.eclipse.core.resources.IMarker;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * YashanDB breakpoint.
 * It contains YashanDB-specific info for IDatabaseBreakpoint
 */
public class YashanDBDebugBreakpointDescriptor implements DBGBreakpointDescriptor {

    private final Object oid;

    private final Object oname;
    private final int lineNo;
    private final boolean onStart;
    private final long targetId;
    private final boolean all;

    public YashanDBDebugBreakpointDescriptor(Object oid,Object oname, int lineNo) {
        this.oid = oid;
        this.oname=oname;
        this.lineNo = lineNo;
        this.onStart = lineNo < 0;
        this.targetId = -1;
        this.all = true;
    }

    public Object getObjectId() {
        return oid;
    }

    public Object getObjectName(){ return oname; }

    public int getLineNo() {
        return lineNo;
    }

    public boolean isOnStart() {
        return onStart;
    }

    public long getTargetId() {
        return targetId;
    }

    public boolean isAll() {
        return all;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(YashanDBDebugConstants.ATTR_FUNCTION_OID, String.valueOf(oid));
        map.put(YashanDBDebugConstants.ATTR_FUNCTION_NAME,String.valueOf(oname));
        map.put("onStart", onStart);
        map.put("targetId", String.valueOf(targetId));
        map.put("all", all);
        return map;
    }

    public static DBGBreakpointDescriptor fromMap(Map<String, Object> attributes) {
        long oid = CommonUtils.toLong(attributes.get(YashanDBDebugConstants.ATTR_FUNCTION_OID));
        String oname=CommonUtils.toString(attributes.get(YashanDBDebugConstants.ATTR_FUNCTION_NAME));
        int parsed = CommonUtils.toInt(attributes.get(IMarker.LINE_NUMBER));
        return new YashanDBDebugBreakpointDescriptor(oid,oname, parsed);
    }

    @Override
    public String toString() {
        return "YashanDBDebugBreakpointDescriptor [oname=" + oname + ", properties=" + toMap() + "]";
    }

}
