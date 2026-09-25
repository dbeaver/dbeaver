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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * One attribute of a Mimer SQL structured type, read from {@code INFORMATION_SCHEMA.ATTRIBUTES}.
 * When {@code DATA_TYPE = 'USER-DEFINED'} the real type is a nested UDT, named via {@code
 * ATTRIBUTE_UDT_SCHEMA}/{@code ATTRIBUTE_UDT_NAME} instead of the usual precision columns.
 *
 * @author Mimer Information Technology
 */
public class MimerUdtAttribute implements DBSObject {

    private final MimerUserDefinedType type;
    private final String name;
    private final int ordinalPosition;
    private final String dataType;
    private final boolean nullable;
    private final String defaultValue;
    private final String collationSchema;
    private final String collationName;

    MimerUdtAttribute(@NotNull MimerUserDefinedType type, @NotNull JDBCResultSet dbResult) {
        this.type = type;
        this.name = JDBCUtils.safeGetString(dbResult, "ATTRIBUTE_NAME");
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION");
        this.nullable = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_NULLABLE"));
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "ATTRIBUTE_DEFAULT");
        this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_SCHEMA");
        this.collationName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_NAME");

        String rawDataType = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
        if ("USER-DEFINED".equalsIgnoreCase(rawDataType)) {
            this.dataType = JDBCUtils.safeGetString(dbResult, "ATTRIBUTE_UDT_SCHEMA") + "."
                + JDBCUtils.safeGetString(dbResult, "ATTRIBUTE_UDT_NAME");
        } else {
            this.dataType = MimerUtils.formatDomainDataType(
                CommonUtils.notEmpty(rawDataType),
                JDBCUtils.safeGetLong(dbResult, "CHARACTER_MAXIMUM_LENGTH"),
                JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION"),
                JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE"));
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Property(viewable = true, order = 3)
    public String getDataType() {
        return dataType;
    }

    @Property(viewable = true, order = 4)
    public boolean isNullable() {
        return nullable;
    }

    @Property(viewable = true, order = 5)
    public String getDefaultValue() {
        return defaultValue;
    }

    @Property(viewable = true, order = 6)
    public String getCollation() {
        if (CommonUtils.isEmpty(collationName)) {
            return null;
        }
        return collationSchema + "." + collationName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return type;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return type.getDataSource();
    }
}
