/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClickhouseTupleType extends ClickhouseAbstractDataType implements DBSEntity {
    private final List<ClickhouseTupleTypeAttribute> attributes;
    private final String name;

    public ClickhouseTupleType(@NotNull ClickhouseDataSource dataSource, @NotNull DBSDataType[] types, @NotNull String[] names) {
        super(dataSource);

        this.attributes = IntStream.range(0, types.length)
            .mapToObj(index -> new ClickhouseTupleTypeAttribute(this, types[index], names[index], index))
            .collect(Collectors.toList());

        this.name = Arrays.stream(types)
            .map(DBSTypedObject::getFullTypeName)
            .collect(Collectors.joining(", ", "Tuple(", ")"));
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public int getTypeID() {
        return Types.STRUCT;
    }

    @Override
    public DBPDataKind getDataKind() {
        return DBPDataKind.STRUCT;
    }

    @NotNull
    public List<ClickhouseTupleTypeAttribute> getAttributes() {
        return attributes;
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) {
        return attributes;
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) {
        return DBUtils.findObject(attributes, attributeName);
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) {
        return null;
    }
}
