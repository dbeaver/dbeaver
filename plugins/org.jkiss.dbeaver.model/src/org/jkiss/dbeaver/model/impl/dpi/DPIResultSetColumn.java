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
package org.jkiss.dbeaver.model.impl.dpi;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetColumn;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class DPIResultSetColumn extends LocalResultSetColumn {
    private final String typeName;
    private final String fullTypeName;
    private final int typeId;
    private final Integer precision;
    private final Integer scale;
    private final long maxLength;
    private final long typeModifiers;

    public DPIResultSetColumn(
        int index,
        String label,
        @NotNull DBSTypedObject typedObject
    ) {
        super(null, index, label, typedObject);
        this.typeName = typedObject.getTypeName();
        this.fullTypeName = typedObject.getFullTypeName();
        this.typeId = typedObject.getTypeID();
        this.precision = typedObject.getPrecision();
        this.scale = typedObject.getScale();
        this.maxLength = typedObject.getMaxLength();
        this.typeModifiers = typedObject.getTypeModifiers();
    }

    @NotNull
    @Override
    public String getTypeName() {
        return typeName;
    }

    @NotNull
    @Override
    public String getFullTypeName() {
        return fullTypeName;
    }

    public int getTypeId() {
        return typeId;
    }

    @Nullable
    @Override
    public Integer getPrecision() {
        return precision;
    }

    @Nullable
    @Override
    public Integer getScale() {
        return scale;
    }

    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public long getTypeModifiers() {
        return typeModifiers;
    }
}
