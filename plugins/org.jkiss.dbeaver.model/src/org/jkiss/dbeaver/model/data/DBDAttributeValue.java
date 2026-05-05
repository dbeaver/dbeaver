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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Column value
 */
public class DBDAttributeValue {

    @NotNull
    private final DBSAttributeBase attribute;
    @Nullable
    private final Object value;

    public DBDAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) {
        this.attribute = attribute;
        this.value = value;
    }

    @NotNull
    public DBSAttributeBase getAttribute() {
        return attribute;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    @Override
    public String toString()
    {
        return attribute.getName() + "=" + value;
    }

    @NotNull
    public static DBSAttributeBase[] getAttributes(@NotNull DBDAttributeValue[] attrValues) {
        final DBSAttributeBase[] attributes = new DBSAttributeBase[attrValues.length];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = attrValues[i].getAttribute();
        }
        return attributes;
    }

    @NotNull
    public static Object[] getValues(@NotNull DBDAttributeValue[] attrValues) {
        final Object[] values = new Object[attrValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = attrValues[i].getValue();
        }
        return values;
    }

    @NotNull
    public static DBSAttributeBase[] getAttributes(@NotNull List<DBDAttributeValue> attrValues) {
        DBSAttributeBase[] attributes = new DBSAttributeBase[attrValues.size()];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = attrValues.get(i).attribute;
        }
        return attributes;
    }

    @NotNull
    public static Object[] getValues(@NotNull List<DBDAttributeValue> attrValues) {
        Object[] values = new Object[attrValues.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = attrValues.get(i).value;
        }
        return values;
    }

    @Nullable
    public static DBDAttributeValue getAttributeValue(
        @NotNull List<DBDAttributeValue> valueList,
        @NotNull DBSEntityAttribute attribute
    ) {
        for (DBDAttributeValue value : valueList) {
            if (CommonUtils.equalObjects(value.attribute.getName(), attribute.getName())) {
                return value;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBDAttributeValue av)) {
            return false;
        }
        if (!CommonUtils.equalObjects(value, av.value)) {
            return false;
        }
        return CommonUtils.equalObjects(attribute.getName(), av.attribute.getName());
    }
}
