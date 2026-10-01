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
import java.util.*;
import java.util.stream.Collectors;

public class DatabricksStructDataType extends DatabricksDataType implements DBSEntity {

    @NotNull
    private final List<DatabricksDataTypeAttribute<DatabricksStructDataType>> attributes;

    DatabricksStructDataType(
        @NotNull GenericStructContainer owner,
        @NotNull Map<String, DatabricksDataType> memberTypesByName,
        boolean isObject
    ) {
        super(owner, Types.STRUCT, prepareName(memberTypesByName, isObject));

        List<DatabricksDataTypeAttribute<DatabricksStructDataType>> attrs = new ArrayList<>();
        for (Map.Entry<String, DatabricksDataType> member : memberTypesByName.entrySet()) {
            attrs.add(new DatabricksDataTypeAttribute<>(this, member.getValue(), member.getKey(), attrs.size()));
        }
        this.attributes = Collections.unmodifiableList(attrs);
    }

    @NotNull
    private static String prepareName(@NotNull Map<String, DatabricksDataType> memberTypesByName, boolean isObject) {
        return (isObject ? "OBJECT" : "STRUCT") + "<" + memberTypesByName.entrySet().stream().map(
            kv -> kv.getKey() + ": " + kv.getValue().getName()
        ).collect(Collectors.joining(", ")) + ">";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DatabricksStructDataType other && this.attributes.equals(other.attributes);
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
