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

import org.jkiss.dbeaver.debug.DBGStackFrame;

public class PostgreDebugStackFrame implements DBGStackFrame {

    private final int level;
    private final String name;
    private final int oid;
    private final int lineNo;
    private final String args;

    public PostgreDebugStackFrame(int level, String name, int oid, int lineNo, String args) {
        super();
        this.level = level;
        this.name = name;
        this.oid = oid;
        this.lineNo = lineNo;
        this.args = args;
    }

    public int getLevel() {
        return level;
    }

    public int getOid() {
        return oid;
    }

    @Override
    public int getLine() {
        return lineNo;
    }

    public String getArgs() {
        return args;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "PostgreDebugStackFrame [level=" + level + ", name=" + name + ", oid=" + oid + ", lineNo=" + lineNo
                + ", args=" + args + "]";
    }



}
