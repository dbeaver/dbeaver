/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;

/**
 * DB2 Type of Trigger Dependency
 *
 * @author Denis Forveille
 */
public enum DB2TriggerDepType {
    A("A (Table alias)", DB2ObjectType.ALIAS),

    B("B (Trigger)", DB2ObjectType.TRIGGER),

    C("C (Column)"),

    F("F (Function)", DB2ObjectType.UDF),

    G("G (Global temporary table)", DB2ObjectType.TABLE),

    H("H (Hierachy table)", DB2ObjectType.TABLE),

    K("K (Package)", DB2ObjectType.PACKAGE),

    L("L (Detached table)", DB2ObjectType.TABLE),

    N("N (Nickname)"),

    O("O (Privilege dependency on all subtables or subviews in a table or view hierarchy)"),

    Q("Q (Sequence)", DB2ObjectType.TABLE),

    R("R (UDT)", DB2ObjectType.UDT),

    S("S (MQT)", DB2ObjectType.SEQUENCE),

    T("T (Table)", DB2ObjectType.TABLE),

    U("U (Typed table)", DB2ObjectType.TABLE),

    V("V (View)", DB2ObjectType.VIEW),

    W("W (Typed view)", DB2ObjectType.VIEW),

    X("X (Index extension)"),

    Z("Z (XSR object)"),

    q("q (Sequence alias)", DB2ObjectType.ALIAS),

    u("u (Module alias)", DB2ObjectType.ALIAS),

    v("v ( Global variable)");

    private String description;
    private DB2ObjectType db2ObjectType;

    // -----------
    // Constructor
    // -----------

    private DB2TriggerDepType(String description, DB2ObjectType db2ObjectType)
    {
        this.description = description;
        this.db2ObjectType = db2ObjectType;
    }

    private DB2TriggerDepType(String description)
    {
        this(description, null);
    }

    // ----------------
    // Standard Getters
    // ----------------

    public String getDescription()
    {
        return description;
    }

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }

}