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

import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.debug.DBGVariableType;

public class PostgreDebugVariable implements DBGVariable<String> {

    private final String name;

    private final String varclass;

    private final int linenumber;

    private final boolean unique;

    private final boolean constant;

    private final boolean notnull;

    private final int oid;

    private final String val;

    @Override
    public String getVal() {

        return val;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public DBGVariableType getType() {
        // TODO Auto-generated method stub
        return DBGVariableType.TEXT;
    }

    public PostgreDebugVariable(String name, String varclass, int linenumber, boolean unique, boolean constant,
                                boolean notnull, int oid, String val) {
        super();
        this.name = name;
        this.varclass = varclass;
        this.linenumber = linenumber;
        this.unique = unique;
        this.constant = constant;
        this.notnull = notnull;
        this.oid = oid;
        this.val = val;
    }

    public String getVarclass() {
        return varclass;
    }

    public int getLinenumber() {
        return linenumber;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isNotnull() {
        return notnull;
    }

    public int getOid() {
        return oid;
    }

    @Override
    public String toString() {
        return "PostgreDebugVariable [name=" + name + ", val=" + val + ", varclass=" + varclass + ", linenumber="
                + linenumber + ", unique=" + unique + ", constant=" + constant + ", notnull=" + notnull + ", oid=" + oid
                + "]";
    }

}
