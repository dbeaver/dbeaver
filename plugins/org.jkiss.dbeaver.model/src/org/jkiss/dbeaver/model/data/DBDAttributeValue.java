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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

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

    public static DBSAttributeBase[] getAttributes(List<DBDAttributeValue> attrValues)
    {
        DBSAttributeBase[] attributes = new DBSAttributeBase[attrValues.size()];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = attrValues.get(i).attribute;
        }
        return attributes;
    }

    public static Object[] getValues(List<DBDAttributeValue> attrValues)
    {
        Object[] values = new Object[attrValues.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = attrValues.get(i).value;
        }
        return values;
    }

}
