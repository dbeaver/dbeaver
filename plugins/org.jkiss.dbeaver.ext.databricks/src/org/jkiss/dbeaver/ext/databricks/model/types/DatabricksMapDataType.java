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

public class DatabricksMapDataType extends DatabricksDataType implements DBSEntity {

    public static class FakeEntryType extends DatabricksDataType implements DBSEntity {

        @NotNull
        private final List<DatabricksDataTypeAttribute<FakeEntryType>> attributes;

        private FakeEntryType(
            @NotNull GenericStructContainer owner,
            @NotNull DatabricksDataType keyType,
            @NotNull DatabricksDataType valueType
        ) {
            super(owner, Types.STRUCT, prepareName(keyType, valueType, false));
            this.attributes = List.of(
                new DatabricksDataTypeAttribute<>(this, keyType, "key", 0),
                new DatabricksDataTypeAttribute<>(this, valueType, "value", 1)
            );
        }

        @NotNull
        public DBSAttributeBase[] getAttributes() {
            return this.attributes.toArray(DBSAttributeBase[]::new);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof FakeEntryType other && this.attributes.equals(other.attributes);
        }

        @NotNull
        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TYPE;
        }

        @Nullable
        @Override
        public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
            return this.attributes;
        }

        @Nullable
        @Override
        public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
            return this.attributes.stream().filter(a -> a.getName().equals(attributeName)).findFirst().orElse(null);
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

    @NotNull
    private final DatabricksDataType keyType;
    @NotNull
    private final DatabricksDataType valueType;

    @NotNull
    private final FakeEntryType entryType;

    DatabricksMapDataType(
        @NotNull GenericStructContainer owner,
        @NotNull DatabricksDataType keyType,
        @NotNull DatabricksDataType valueType
    ) {
        super(owner, Types.ARRAY, prepareName(keyType, valueType, true));
        this.keyType = keyType;
        this.valueType = valueType;
        this.entryType = new FakeEntryType(owner, keyType, valueType);
    }

    @NotNull
    public DatabricksDataType getKeyType() {
        return this.keyType;
    }

    @NotNull
    public DatabricksDataType getValueType() {
        return this.valueType;
    }

    @NotNull
    public FakeEntryType getEntryType() {
        return this.entryType;
    }

    @NotNull
    private static String prepareName(@NotNull DatabricksDataType keyType, @NotNull DatabricksDataType valueType, boolean isMap) {
        return (isMap ? "MAP" : "MAP.ENTRY ") + "<" + keyType.getName() + ", " + valueType.getName() + ">";
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) {
        return this.entryType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DatabricksMapDataType other && this.entryType.equals(other.entryType);
    }


    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return this.entryType.getEntityType();
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.entryType.getAttributes(monitor);
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return this.entryType.getAttribute(monitor, attributeName);
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
