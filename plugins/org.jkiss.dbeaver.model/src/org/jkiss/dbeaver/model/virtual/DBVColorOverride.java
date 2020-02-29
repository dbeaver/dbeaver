/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
    private boolean isRange;
    private boolean singleColumn;
    private DBCLogicalOperator operator;
    private Object[] attributeValues;
    private String colorForeground, colorForeground2;
    private String colorBackground, colorBackground2;

    public DBVColorOverride(String attributeName, DBCLogicalOperator operator, Object[] attributeValues, String colorForeground, String colorBackground) {
        this.attributeName = attributeName;
        this.operator = operator;
        this.attributeValues = attributeValues;
        this.colorForeground = colorForeground;
        this.colorBackground = colorBackground;
    }

    public DBVColorOverride(DBVColorOverride source) {
        this.attributeName = source.attributeName;
        this.isRange = source.isRange;
        this.singleColumn = source.singleColumn;
        this.operator = source.operator;
        this.attributeValues = source.attributeValues == null ? null : Arrays.copyOf(source.attributeValues, source.attributeValues.length);
        this.colorForeground = source.colorForeground;
        this.colorBackground = source.colorBackground;
        this.colorForeground2 = source.colorForeground2;
        this.colorBackground2 = source.colorBackground2;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public boolean isRange() {
        return isRange;
    }

    public void setRange(boolean range) {
        isRange = range;
    }

    public boolean isSingleColumn() {
        return singleColumn;
    }

    public void setSingleColumn(boolean singleColumn) {
        this.singleColumn = singleColumn;
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

    public void setAttributeValues(Object[] attributeValues) {
        this.attributeValues = attributeValues;
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

    public String getColorForeground2() {
        return colorForeground2;
    }

    public void setColorForeground2(String colorForeground2) {
        this.colorForeground2 = colorForeground2;
    }

    public String getColorBackground() {
        return colorBackground;
    }

    public void setColorBackground(String colorBackground) {
        this.colorBackground = colorBackground;
    }

    public String getColorBackground2() {
        return colorBackground2;
    }

    public void setColorBackground2(String colorBackground2) {
        this.colorBackground2 = colorBackground2;
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

