/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
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

    CPMA("Page map index", false),

    DIM("Dimension Block Index", false),

    RCT("Key Sequence Index", false),

    REG("Regular", true),

    TEXT("Text Index", false),

    XPTH("XML path Index", false),

    XRGN("XML region Index", false),

    XVIL("Index over XML column (logical)", false),

    XVIP("Index over XML column (physical)", false);

    private String       name;
    private DBSIndexType dbsIndexType;
    private Boolean      validForCreation;

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
    @NotNull
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