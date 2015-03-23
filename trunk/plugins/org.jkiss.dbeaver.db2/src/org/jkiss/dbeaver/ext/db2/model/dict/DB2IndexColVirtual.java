/*
 * Copyright (C) 2013-2015 Denis Forveille titou10.titou10@gmail.com
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

/**
 * DB2 Index Virtual status
 * 
 * @author Denis Forveille
 */
public enum DB2IndexColVirtual implements DBPNamedObject {
    N("", false),

    S("Virtual Index Column", true),

    Y("Virtual Index Column not in this Table", true);

    private String name;
    private Boolean virtual;

    // -----------------
    // Constructor
    // -----------------
    private DB2IndexColVirtual(String name, Boolean virtual)
    {
        this.name = name;
        this.virtual = virtual;
    }

    // -----------------------
    // Helpers
    // -----------------------

    public Boolean isNotVirtual()
    {
        return !virtual;
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

    public Boolean isVirtual()
    {
        return virtual;
    }

}