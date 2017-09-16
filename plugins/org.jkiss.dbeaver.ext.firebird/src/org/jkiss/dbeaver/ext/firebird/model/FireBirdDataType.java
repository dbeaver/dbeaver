/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.meta.Property;

public class FireBirdDataType extends GenericDataType {

    private final FireBirdFieldType fieldType;
    private final int subType;
    private int fieldLength;
    private int charLength;
    private String computedSource;
    private String validationSource;
    private String defaultSource;
    private String charsetName;
    private boolean notNull;

    public FireBirdDataType(FireBirdDataSource genericDataSource, FireBirdFieldType fieldType, int subType, String name, String remarks,
        boolean unsigned, boolean searchable, int precision, int minScale, int maxScale,
        int fieldLength, int charLength,
        String computedSource, String validationSource, String defaultSource, String charsetName, boolean notNull)
    {
        super(genericDataSource, fieldType.getValueType(), name, remarks, unsigned, searchable, precision, minScale, maxScale);

        this.fieldType = fieldType;
        this.subType = subType;
        this.fieldLength = fieldLength;
        this.charLength = charLength;
        this.computedSource = computedSource;
        this.validationSource = validationSource;
        this.defaultSource = defaultSource;
        this.charsetName = charsetName;
        this.notNull = notNull;
    }

    public FireBirdDataType(FireBirdDataSource dataSource, FireBirdFieldType fieldType) {
        super(dataSource, fieldType.getValueType(), fieldType.getName(), null, false, true, 0, 0, 0);

        this.fieldType = fieldType;
        this.subType = 0;
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

}
