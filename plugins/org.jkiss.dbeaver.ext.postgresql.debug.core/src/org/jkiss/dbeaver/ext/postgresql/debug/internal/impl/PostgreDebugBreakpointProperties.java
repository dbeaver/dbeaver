/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.debug.DBGBreakpointProperties;

public class PostgreDebugBreakpointProperties implements DBGBreakpointProperties {

    private final long lineNo;
    private final boolean onStart;
    private final long targetId;
    private final boolean all;
    private final boolean global;

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

    public boolean isGlobal() {
        return global;
    }

    public PostgreDebugBreakpointProperties(long lineNo, long targetId, boolean global) {
        this.lineNo = lineNo;
        this.onStart = lineNo < 0;
        this.targetId = targetId;
        this.all = targetId < 0;
        this.global = global;
    }

    public PostgreDebugBreakpointProperties(long lineNo, boolean global) {
        this.lineNo = lineNo;
        this.onStart = lineNo < 0;
        this.targetId = -1;
        this.all = true;
        this.global = global;
    }

    public PostgreDebugBreakpointProperties(boolean global) {
        this.lineNo = -1;
        this.onStart = true;
        this.targetId = -1;
        this.all = true;
        this.global = global;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("lineNo", lineNo);
        map.put("onStart", onStart);
        map.put("targetId", targetId);
        map.put("all", all);
        map.put("global", global);
        return map;
    }

    @Override
    public String toString() {
        return "PostgreDebugBreakpointProperties [lineNo=" + lineNo + ", onStart=" + onStart + ", targetId=" + targetId
                + ", all=" + all + ", global=" + global + "]";
    }

}
