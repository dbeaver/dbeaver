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
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * DB2 Type of Indexes
 * 
 * @author Denis Forveille
 */
public enum DB2IndexType implements DBPNamedObject {
    BLOK("Block Index", false),

    CLUS("Clustering Index", true),

    DIM("Dimension Block Index", false),

    RCT("Key Sequence Index", false),

    REG("Regular", true),

    TEXT("Text Index", false),

    XPTH("XML path Index", false),

    XRGN("XML region Index", false),

    XVIL("Index over XML column (logical)", false),

    XVIP("Index over XML column (physical)", false);

    private String name;
    private DBSIndexType dbsIndexType;
    private Boolean validForCreation;

    // -----------------
    // Constructor
    // -----------------
    private DB2IndexType(String name, Boolean validForCreation)
    {
        this.name = name;
        this.validForCreation = validForCreation;
        this.dbsIndexType = new DBSIndexType(this.name(), name);
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

    public DBSIndexType getDBSIndexType()
    {
        return dbsIndexType;
    }

    public Boolean isValidForCreation()
    {
        return validForCreation;
    }
}