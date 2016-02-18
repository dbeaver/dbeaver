/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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