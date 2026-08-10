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
package org.jkiss.dbeaver.ext.databricks.model.types;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DatabricksArrayOfEntitiesDataType extends DatabricksDataType implements DBSEntity {

    @NotNull
    private final DBSEntity itemType;

    DatabricksArrayOfEntitiesDataType(
        @NotNull GenericStructContainer owner,
        @NotNull DBSEntity itemType
    ) {
        super(owner, Types.ARRAY, "ARRAY<" + itemType.getName() + ">");
        this.itemType = itemType;
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) {
        return this.itemType instanceof DBSDataType t ? t : null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DatabricksArrayOfEntitiesDataType other
            && this.itemType.getClass().equals(other.itemType.getClass())
            && this.itemType.equals(other.itemType);
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return this.itemType.getEntityType();
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.itemType.getAttributes(monitor);
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return this.itemType.getAttribute(monitor, attributeName);
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
