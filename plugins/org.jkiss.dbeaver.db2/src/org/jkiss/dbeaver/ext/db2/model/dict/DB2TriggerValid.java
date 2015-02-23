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

/**
 * DB2 Trigger Valid attribute
 * 
 * @author Denis Forveille
 */
public enum DB2TriggerValid implements DBPNamedObject {
    N("Invalid", DBSObjectState.INVALID),

    X("Inoperative", DBSObjectState.INVALID), // DF: No exact correspondance

    Y("Valid", DBSObjectState.ACTIVE); // DF: No exact correspondance

    private String name;
    private DBSObjectState state;

    // -----------------
    // Constructor
    // -----------------

    private DB2TriggerValid(String name, DBSObjectState state)
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