/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2 Table Type
 * 
 * @author Denis Forveille
 */
public enum DB2TableType implements DBPNamedObject {

    A("Alias", DB2ObjectType.ALIAS),

    G("Created temporary table", DB2ObjectType.TABLE),

    H("Hierarchy table", DB2ObjectType.TABLE),

    L("Detached table", DB2ObjectType.TABLE),

    N("Nickname", DB2ObjectType.NICKNAME),

    S("Materialized Query Table", DB2ObjectType.MQT),

    T("Table (untyped)", DB2ObjectType.TABLE),

    U("Inoperative", DB2ObjectType.TABLE),

    V("View (untyped)", DB2ObjectType.VIEW),

    W("Typed view", DB2ObjectType.VIEW);

    private String                                  description;
    private DB2ObjectType                           db2ObjectType;

    private static final Map<DB2ObjectType, String> IN_CLAUSE_CACHE = new HashMap<>();

    // -----------------
    // Constructor
    // -----------------
    private DB2TableType(String description, DB2ObjectType db2ObjectType)
    {
        this.description = description;
        this.db2ObjectType = db2ObjectType;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return description;
    }

    public static String getInClause(DB2ObjectType objectType)
    {
        String inClause = IN_CLAUSE_CACHE.get(objectType);
        if (inClause == null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("(");
            for (DB2TableType db2TableType : values()) {
                if (db2TableType.getDb2ObjectType() == objectType) {
                    sb.append("'").append(db2TableType.name()).append("'");
                    sb.append(",");
                }
            }
            // Remove last "," eventually
            inClause = "()";
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
                sb.append(")");
                inClause = sb.toString();
            }
            IN_CLAUSE_CACHE.put(objectType, inClause);
        }
        return inClause;
    }

    // ----------------
    // Standard Getters
    // ----------------
    @NotNull
    @Override
    public String getName()
    {
        return description; // DF: yes strange getter..
    }

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }
}