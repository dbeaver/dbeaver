/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import org.eclipse.core.resources.IMarker;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.ext.postgresql.debug.PostgreDebugConstants;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * PG breakpoint.
 * It contains PG-specific info for IDatabaseBreakpoint
 */
public class PostgreDebugBreakpointDescriptor implements DBGBreakpointDescriptor {

    private final Object oid;
    private final long lineNo;
    private final boolean onStart;
    private final long targetId;
    private final boolean all;

    public PostgreDebugBreakpointDescriptor(Object oid, long lineNo) {
        this.oid = oid;
        this.lineNo = lineNo;
        this.onStart = lineNo < 0;
        this.targetId = -1;
        this.all = true;
    }

    public Object getObjectId() {
        return oid;
    }

    public long getLineNo() {
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
        map.put(PostgreDebugConstants.ATTR_FUNCTION_OID, String.valueOf(oid));
        map.put("onStart", onStart);
        map.put("targetId", String.valueOf(targetId));
        map.put("all", all);
        return map;
    }

    public static DBGBreakpointDescriptor fromMap(Map<String, Object> attributes) {
        long oid = CommonUtils.toLong(attributes.get(PostgreDebugConstants.ATTR_FUNCTION_OID));
        long parsed = CommonUtils.toLong(attributes.get(IMarker.LINE_NUMBER));
        return new PostgreDebugBreakpointDescriptor(oid, parsed);
    }

    @Override
    public String toString() {
        return "PostgreDebugBreakpointDescriptor [obj=" + oid + ", properties=" + toMap() + "]";
    }

}
