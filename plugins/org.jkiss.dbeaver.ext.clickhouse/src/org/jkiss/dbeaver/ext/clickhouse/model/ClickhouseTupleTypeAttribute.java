/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;

public class ClickhouseTupleTypeAttribute extends AbstractAttribute implements DBSEntityAttribute, DBSTypedObjectEx {
    private final ClickhouseTupleType tupleType;
    private final DBSDataType attributeType;

    public ClickhouseTupleTypeAttribute(
        @NotNull ClickhouseTupleType tuple,
        @NotNull DBSDataType attr,
        @NotNull String name,
        int position
    ) {
        super(name, attr.getTypeName(), attr.getTypeID(), position, -1, null, null, false, false);
        this.tupleType = tuple;
        this.attributeType = attr;
    }

    @NotNull
    @Override
    public DBSEntity getParentObject() {
        return tupleType;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return tupleType.getDataSource();
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return attributeType.getDataKind();
    }

    @Nullable
    @Override
    public String getDefaultValue() {
        return null;
    }

    @NotNull
    @Override
    public DBSDataType getDataType() {
        return attributeType;
    }
}
