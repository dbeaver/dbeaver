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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Default value handler
 */
public class DefaultValueHandler extends BaseValueHandler {

    public static final DefaultValueHandler INSTANCE = new DefaultValueHandler();

    @NotNull
    @Override
    public Class<Object> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Object.class;
    }

    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index) throws DBCException
    {
        return resultSet.getAttributeValue(index);
    }

    @Override
    public void bindValueObject(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull DBSTypedObject type,
        int index,
        Object value) throws DBCException
    {
        throw new DBCException("Object parameter [" + DBUtils.getDefaultValueDisplayString(value, DBDDisplayFormat.UI) + "] binding not supported");
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        return object;
    }

}
