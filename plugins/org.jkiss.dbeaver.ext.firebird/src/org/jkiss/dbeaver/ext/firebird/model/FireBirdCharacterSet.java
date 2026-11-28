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

import java.util.List;

/**
 * Firebird character set, stored in RDB$CHARACTER_SETS.
 */
public class FireBirdCharacterSet implements DBSObject {

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    private final int id;
    private final int bytesPerCharacter;
    @Nullable
    private final String defaultCollationName;
    @Nullable
    private final String description;

    public FireBirdCharacterSet(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        int id,
        int bytesPerCharacter,
        @Nullable String defaultCollationName,
        @Nullable String description
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.id = id;
        this.bytesPerCharacter = bytesPerCharacter;
        this.defaultCollationName = defaultCollationName;
        this.description = description;
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

    @Property(viewable = true, order = 3)
    public int getBytesPerCharacter() {
        return bytesPerCharacter;
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getDefaultCollationName() {
        return defaultCollationName;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    /**
     * Collations defined for this character set.
     */
    @NotNull
    public List<FireBirdCollation> getCollations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return dataSource.getCollations(monitor).stream()
            .filter(collation -> collation.getCharacterSetId() == id)
            .toList();
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
