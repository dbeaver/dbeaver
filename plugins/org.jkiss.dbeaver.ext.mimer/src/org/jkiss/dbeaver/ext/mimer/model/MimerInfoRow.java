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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One name/value row shown under the "Server Info &gt; Server" and "Server Info &gt; Driver"
 * nodes - the same "folder shows a Property/Value grid" shape as the SQL Standards folders (see
 * {@link MimerSqlStandardRow}), so those three sibling nodes look consistent and there's no
 * redundant folder-then-leaf nesting. Every value comes from JDBC metadata DBeaver already
 * cached at connect time (plus the driver descriptor) - see {@link MimerDataSource#getServerInfoRows}
 * / {@link MimerDataSource#getDriverInfoRows}. Deliberately no {@code SYSTEM.*} queries, since an
 * ordinary (non-DBA) ident has no access to those; Mimer SQL also exposes no host-OS / platform
 * value through JDBC metadata, so that isn't shown.
 *
 * @author Mimer Information Technology
 */
public class MimerInfoRow implements DBSObject {

    private final DBSObject owner;
    private final String name;
    private final String value;

    public MimerInfoRow(@NotNull DBSObject owner, @NotNull String name, @Nullable String value) {
        this.owner = owner;
        this.name = name;
        this.value = value;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getValue() {
        return value;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) owner.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }
}
