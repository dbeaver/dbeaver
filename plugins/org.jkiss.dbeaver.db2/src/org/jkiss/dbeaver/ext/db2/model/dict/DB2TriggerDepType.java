/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
public enum DB2TriggerDepType implements DBPNamedObject {
    A("Table alias", DB2ObjectType.ALIAS),

    B("Trigger", DB2ObjectType.TRIGGER),

    C("Column", DB2ObjectType.COLUMN),

    F("Routine", DB2ObjectType.ROUTINE),

    G("Global temporary table", DB2ObjectType.TABLE),

    H("Hierachy table", DB2ObjectType.TABLE),

    K("Package", DB2ObjectType.PACKAGE),

    L("Detached table", DB2ObjectType.TABLE),

    N("Nickname", DB2ObjectType.NICKNAME),

    O("Privilege dependency on all subtables or subviews in a table or view hierarchy"),

    Q("Sequence", DB2ObjectType.SEQUENCE),

    R("UDT", DB2ObjectType.UDT),

    S("MQT", DB2ObjectType.MQT),

    T("Table", DB2ObjectType.TABLE),

    U("Typed table", DB2ObjectType.TABLE),

    V("View", DB2ObjectType.VIEW),

    W("Typed view", DB2ObjectType.VIEW),

    X("Index extension"),

    Z("XSR object", DB2ObjectType.XML_SCHEMA),

    q("Sequence alias", DB2ObjectType.ALIAS),

    u("Module alias", DB2ObjectType.ALIAS),

    v("Global variable", DB2ObjectType.VARIABLE);

    private String name;
    private DB2ObjectType db2ObjectType;

    // -----------
    // Constructor
    // -----------

    private DB2TriggerDepType(String name, DB2ObjectType db2ObjectType)
    {
        this.name = name;
        this.db2ObjectType = db2ObjectType;
    }

    private DB2TriggerDepType(String description)
    {
        this(description, null);
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

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }

    @Override
    public String getName()
    {
        return name;
    }

}