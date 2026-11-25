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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Firebird collation, stored in RDB$COLLATIONS.
 */
public class FireBirdCollation implements DBSObject {

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    private final int id;
    private final int characterSetId;
    @Nullable
    private final String baseCollationName;
    @Nullable
    private final String specificAttributes;
    @Nullable
    private final String description;
    private final boolean system;

    public FireBirdCollation(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        int id,
        int characterSetId,
        @Nullable String baseCollationName,
        @Nullable String specificAttributes,
        @Nullable String description,
        boolean system
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.id = id;
        this.characterSetId = characterSetId;
        this.baseCollationName = baseCollationName;
        this.specificAttributes = specificAttributes;
        this.description = description;
        this.system = system;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getId() {
        return id;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public FireBirdCharacterSet getCharacterSet(@NotNull DBRProgressMonitor monitor) throws DBException {
        return dataSource.getCharacterSet(monitor, characterSetId);
    }

    public int getCharacterSetId() {
        return characterSetId;
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getBaseCollationName() {
        return baseCollationName;
    }

    @Nullable
    @Property(viewable = true, order = 5)
    public String getSpecificAttributes() {
        return specificAttributes;
    }

    /**
     * System collations are built in; user-defined ones are created with CREATE COLLATION.
     * Not exposed as {@link org.jkiss.dbeaver.model.DBPSystemObject}, otherwise built-in collations
     * would be hidden whenever system objects are not shown.
     */
    @Property(viewable = true, order = 6)
    public boolean isSystem() {
        return system;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public FireBirdDataSource getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public FireBirdDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String toString() {
        return name;
    }
}
