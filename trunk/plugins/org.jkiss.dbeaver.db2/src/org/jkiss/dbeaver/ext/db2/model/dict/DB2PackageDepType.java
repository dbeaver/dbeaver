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
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Type of Trigger Dependency
 * 
 * @author Denis Forveille
 */
public enum DB2PackageDepType implements DBPNamedObject {
    A("A (Table alias)", DB2ObjectType.ALIAS),

    B("B (Trigger)", DB2ObjectType.TRIGGER),

    D("D (Server)"),

    F("F (Function)", DB2ObjectType.UDF),

    G("G (Global temporary table)", DB2ObjectType.TABLE),

    I("I (Index)", DB2ObjectType.INDEX),

    M("M (Function Mapping)"),

    N("N (Nickname)"),

    O("O (Privilege dependency on all subtables or subviews in a table or view hierarchy)"),

    P("P (Page Size)"),

    Q("Q (Sequence)", DB2ObjectType.SEQUENCE),

    R("R (UDT)", DB2ObjectType.UDT),

    S("S (MQT)", DB2ObjectType.TABLE),

    T("T (Table)", DB2ObjectType.TABLE),

    U("U (Typed table)", DB2ObjectType.TABLE),

    V("V (View)", DB2ObjectType.VIEW),

    W("W (Typed view)", DB2ObjectType.VIEW),

    X("X (Index extension)"),

    Z("Z (XSR object)"),

    q("q (Sequence alias)", DB2ObjectType.ALIAS),

    u("u (Module alias)", DB2ObjectType.ALIAS),

    v("v ( Global variable)");

    private String name;
    private DB2ObjectType db2ObjectType;

    // -----------
    // Constructor
    // -----------

    private DB2PackageDepType(String name, DB2ObjectType db2ObjectType)
    {
        this.name = name;
        this.db2ObjectType = db2ObjectType;
    }

    private DB2PackageDepType(String name)
    {
        this(name, null);
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

    @Override
    public String getName()
    {
        return name;
    }

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }

}