/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
}

