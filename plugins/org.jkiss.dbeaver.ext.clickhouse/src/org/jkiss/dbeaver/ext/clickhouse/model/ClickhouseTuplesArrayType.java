/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClickhouseTuplesArrayType<T extends DBSDataType & DBSEntity> extends ClickhouseArrayType implements DBSEntity {

    private final T componentType;

    public ClickhouseTuplesArrayType(@NotNull ClickhouseDataSource dataSource, @NotNull T componentType) {
        super(dataSource, componentType, "Array", "Array(" + componentType.getFullTypeName() + ")");
        this.componentType = componentType;
    }

    protected ClickhouseTuplesArrayType(
        @NotNull ClickhouseDataSource dataSource,
        @NotNull T componentType,
        @NotNull String baseName,
        @NotNull String fullName
    ) {
        super(dataSource, componentType, baseName, fullName);
        this.componentType = componentType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ClickhouseTuplesArrayType<?> other && this.componentType.equals(other.componentType);
    }

    @NotNull
    @Override
    public T getComponentType() {
        return this.componentType;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return this.componentType.getEntityType();
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.componentType.getAttributes(monitor);
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return this.componentType.getAttribute(monitor, attributeName);
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }
}
