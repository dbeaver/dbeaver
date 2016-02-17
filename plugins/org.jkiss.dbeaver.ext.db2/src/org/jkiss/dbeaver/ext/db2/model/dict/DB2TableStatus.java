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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.DBIcon;

/**
 * DB2 Table Status
 * 
 * @author Denis Forveille
 */
public enum DB2TableStatus implements DBPNamedObject {
    C("Set integrity pending", new DBSObjectState("Set Integrity Pending", DBIcon.OVER_ERROR)),

    N("Normal", DBSObjectState.NORMAL),

    X("Inoperative", new DBSObjectState("Inoperative", DBIcon.OVER_ERROR));

    private String name;
    private DBSObjectState state;

    // -----------------
    // Constructor
    // -----------------
    private DB2TableStatus(String name, DBSObjectState state)
    {
        this.name = name;
        this.state = state;
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

    public DBSObjectState getState()
    {
        return state;
    }
}