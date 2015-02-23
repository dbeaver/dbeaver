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

import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.ui.DBIcon;

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