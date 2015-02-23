/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

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
