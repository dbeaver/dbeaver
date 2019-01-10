/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;

import java.util.Arrays;

/**
 * Color override settings
 */
public class DBVColorOverride {
    private String attributeName;
    private DBCLogicalOperator operator;
    private Object[] attributeValues;
    private String colorForeground;
    private String colorBackground;

    public DBVColorOverride(String attributeName, DBCLogicalOperator operator, Object[] attributeValues, String colorForeground, String colorBackground) {
        this.attributeName = attributeName;
        this.operator = operator;
        this.attributeValues = attributeValues;
        this.colorForeground = colorForeground;
        this.colorBackground = colorBackground;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public DBCLogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(DBCLogicalOperator operator) {
        this.operator = operator;
    }

    public Object[] getAttributeValues() {
        return attributeValues;
    }

    public void addAttributeValue(Object value) {
        if (this.attributeValues == null) {
            this.attributeValues = new Object[] { value };
        } else {
            Object[] newValue = new Object[this.attributeValues.length + 1];
            System.arraycopy(attributeValues, 0, newValue, 0, attributeValues.length);
            newValue[newValue.length - 1] = value;
            attributeValues = newValue;
        }
    }

    public String getColorForeground() {
        return colorForeground;
    }

    public void setColorForeground(String colorForeground) {
        this.colorForeground = colorForeground;
    }

    public String getColorBackground() {
        return colorBackground;
    }

    public void setColorBackground(String colorBackground) {
        this.colorBackground = colorBackground;
    }

    public boolean matches(@NotNull String attrName, @NotNull DBCLogicalOperator operator, @Nullable Object[] values) {
        return
            attrName.equals(this.attributeName) &&
            operator == this.operator &&
            Arrays.equals(this.attributeValues, values);
    }

    @Override
    public String toString() {
        return attributeName + " " + operator.toString() + " " + Arrays.toString(attributeValues);
    }
}

