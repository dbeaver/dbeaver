/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Explain Operator Type
 * 
 * @author Denis Forveille
 */
public enum DB2PlanOperatorType implements DBPNamedObject {
    DELETE("Delete"),

    EISCAN("Extended Index Scan"),

    FETCH("Fetch"),

    FILTER("Filter rows"),

    GENROW("Generate Row"),

    GRPBY("Group By"),

    HSJOIN("Hash Join"),

    INSERT("Insert"),

    IXAND("Dynamic Bitmap Index ANDing"),

    IXSCAN("Index scan"),

    MSJOIN("Merge Scan Join"),

    NLJOIN("Nested loop Join"),

    REBAL("Rebalance rows between SMP subagents"),

    RETURN("Return"),

    RIDSCN("RID Scan"),

    RPD("Remote PushDown"),

    SHIP("Ship query to remote system"),

    SORT("Sort"),

    TBFUNC("In-stream table function operator"),

    TBSCAN("Table Scan"),

    TEMP("Temporary Table Construction"),

    TQ("Table Queue"),

    UNION("Union"),

    UNIQUE("Duplicate Elimination"),

    UPDATE("Update"),

    XISCAN("Index scan over XML data"),

    XSCAN("XML document navigation scan"),

    XANDOR("Index ANDing and ORing over XML data"),

    ZZJOIN("Zigzag join");

    private String name;

    // -----------------
    // Constructor
    // -----------------
    private DB2PlanOperatorType(String name)
    {
        this.name = name;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------
    @NotNull
    @Override
    public String getName()
    {
        return name;
    }
}