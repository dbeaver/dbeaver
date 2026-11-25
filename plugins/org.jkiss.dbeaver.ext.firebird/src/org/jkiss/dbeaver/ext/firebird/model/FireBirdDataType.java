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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.meta.Property;

public class FireBirdDataType extends GenericDataType implements DBPSystemObject {

    private final FireBirdFieldType fieldType;
    private final int subType;
    private final boolean domain;
    private boolean systemDomain;
    private int fieldLength;
    private int charLength;
    private String computedSource;
    private String validationSource;
    private String defaultSource;
    private String charsetName;
    private boolean notNull;

    /**
     * Creates a domain read from RDB$FIELDS. The remaining domain attributes are set with setters.
     */
    public FireBirdDataType(
        @NotNull FireBirdDataSource dataSource,
        @NotNull FireBirdFieldType fieldType,
        int subType,
        @NotNull String name,
        @Nullable String remarks,
        int precision,
        int scale
    ) {
        super(dataSource, fieldType.getValueType(), name, remarks, false, true, precision, scale, scale);

        this.fieldType = fieldType;
        this.subType = subType;
        this.domain = true;
    }

    public FireBirdDataType(@NotNull FireBirdDataSource dataSource, @NotNull FireBirdFieldType fieldType) {
        super(dataSource, fieldType.getValueType(), fieldType.getName(), null, false, true, 0, 0, 0);

        this.fieldType = fieldType;
        this.subType = 0;
        this.domain = false;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        if (fieldType == FireBirdFieldType.CHAR &&
            (FireBirdConstants.CHARSET_OCTETS.equals(charsetName) || FireBirdConstants.CHARSET_BINARY.equals(charsetName)))
        {
            return DBPDataKind.BINARY;
        }
        return super.getDataKind();
    }

    @Property(order = 70)
    public int getSubType() {
        return subType;
    }

    @Property(order = 50)
    public String getValidationSource() {
        return validationSource;
    }

    @Property(order = 51)
    public String getComputedSource() {
        return computedSource;
    }

    @Property(order = 45)
    public String getDefaultSource() {
        return defaultSource;
    }

    @Property(order = 60)
    public String getCharsetName() {
        return charsetName;
    }

    @Property(order = 16)
    public String getFieldType() {
        return fieldType.getName();
    }

    @Property(order = 17)
    public int getFieldLength() {
        return fieldLength;
    }

    @Property(order = 18)
    public int getCharLength() {
        return charLength;
    }

    @Property(order = 19)
    public boolean isNotNull() {
        return notNull;
    }

    /**
     * Returns true if this type is a domain read from RDB$FIELDS (including system and implicit domains),
     * false for built-in types.
     */
    public boolean isDomain() {
        return domain;
    }

    /**
     * System domains (RDB$SYSTEM_FLAG set, e.g. RDB$..., MON$..., SEC$...) and the implicit RDB$&lt;n&gt; domains
     * Firebird creates for columns with a built-in type. User-defined domains may contain '$' as well (DOM$USD).
     */
    @Override
    public boolean isSystem() {
        return systemDomain || (domain && getTypeName().startsWith(FireBirdConstants.SYSTEM_OBJECT_PREFIX));
    }

    public void setFieldLength(int fieldLength) {
        this.fieldLength = fieldLength;
    }

    public void setCharLength(int charLength) {
        this.charLength = charLength;
    }

    public void setComputedSource(@Nullable String computedSource) {
        this.computedSource = computedSource;
    }

    public void setValidationSource(@Nullable String validationSource) {
        this.validationSource = validationSource;
    }

    public void setDefaultSource(@Nullable String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public void setCharsetName(@Nullable String charsetName) {
        this.charsetName = charsetName;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public void setSystemDomain(boolean systemDomain) {
        this.systemDomain = systemDomain;
    }
}
