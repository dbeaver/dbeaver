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
 * DB2 Table Type
 *
 * @author Denis Forveille
 */
public enum DB2TableType {

    A("A (Alias)", DB2ObjectType.ALIAS),

    G("G (Created temporary table)", DB2ObjectType.TABLE),

    H("H (Hierarchy table)", DB2ObjectType.TABLE),

    L("L (Detached table)", DB2ObjectType.TABLE),

    N("N (Nickname)", DB2ObjectType.ALIAS), // TODO DF: Wrong will be NICKNAME in the future..

    S("S (Materialized query table)", DB2ObjectType.TABLE),

    T("T (Table (untyped))", DB2ObjectType.TABLE),

    U("U (Inoperative)", DB2ObjectType.TABLE),

    V("V (View (untyped))", DB2ObjectType.VIEW),

    W("W (Typed view)", DB2ObjectType.VIEW);

    private String description;
    private DB2ObjectType db2ObjectType;

    // -----------------
    // Constructor
    // -----------------
    private DB2TableType(String description, DB2ObjectType db2ObjectType)
    {
        this.description = description;
        this.db2ObjectType = db2ObjectType;
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