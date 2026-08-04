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
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Types;
import java.util.Map;
import java.util.Objects;

public class DatabricksDataType extends GenericDataType {

    protected DatabricksDataType(
        @NotNull GenericStructContainer owner,
        int jdbcTypeKind,
        @NotNull String name
    ) {
        super(owner, jdbcTypeKind, name, null, false, false, 0, 0, 0);
    }

    DatabricksDataType(
        @NotNull GenericStructContainer owner,
        int jdbcTypeKind,
        @NotNull String name,
        int precision, int scale
    ) {
        super(owner, jdbcTypeKind, prepareName(jdbcTypeKind, name, precision, scale), null, false, false, precision, scale, scale);
    }

    @NotNull
    private static String prepareName(int jdbcTypeKind, @NotNull String name, int precision, int scale) {
        return jdbcTypeKind != Types.DECIMAL ? name : (name + "(" + precision + ", " + scale + ")");
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof DatabricksDataType other &&
            Objects.equals(this.getPrecision(), other.getPrecision()) &&
            this.getMaxScale() == other.getMaxScale() &&
            this.getMinScale() == other.getMinScale() &&
            this.getTypeID() == other.getTypeID();
    }
}
