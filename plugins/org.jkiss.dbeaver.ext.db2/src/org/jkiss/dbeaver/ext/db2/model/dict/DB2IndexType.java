/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

    private final String title;
    private final DBSIndexType dbsIndexType;
    private final Boolean validForCreation;

    DB2IndexType(String title, Boolean validForCreation) {
        this.title = title;
        this.validForCreation = validForCreation;
        this.dbsIndexType = new DBSIndexType(this.name(), title);
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public DBSIndexType getDBSIndexType() {
        return dbsIndexType;
    }

    public Boolean isValidForCreation() {
        return validForCreation;
    }
}