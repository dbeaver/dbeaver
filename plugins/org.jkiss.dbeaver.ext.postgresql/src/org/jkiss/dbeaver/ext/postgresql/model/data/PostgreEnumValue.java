/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreOid;
import org.jkiss.dbeaver.model.data.DBDEnum;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

/**
 * Enum type
 */
public class PostgreEnumValue implements DBDEnum {

    private PostgreAttribute attribute;
    private String value;

    public PostgreEnumValue(PostgreEnumValue source)
    {
        this.attribute = source.attribute;
        this.value = source.value;
    }

    public PostgreEnumValue(PostgreAttribute attribute, @Nullable String value)
    {
        this.attribute = attribute;
        this.value = value;
    }

    public PostgreAttribute getAttribute()
    {
        return attribute;
    }

    @Override
    public String getValue()
    {
        return value;
    }

    @NotNull
    @Override
    public DBSDataType getElementType() {
        return attribute.getDatabase().dataTypeCache.getDataType(PostgreOid.VARCHAR);
    }

    @Override
    public Object[] getEnumElements() {
        return attribute.getDataType().getEnumValues();
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull()
    {
        return value == null;
    }

    @Override
    public void release()
    {
        // do nothing
    }

    @Override
    public int hashCode()
    {
        return value == null ? super.hashCode() : value.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj ||
            (obj instanceof PostgreEnumValue && CommonUtils.equalObjects(((PostgreEnumValue) obj).getValue(), value));
    }

}
