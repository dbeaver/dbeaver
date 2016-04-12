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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Proxy value handler
 */
public class ProxyValueHandler implements DBDValueHandler {

    private final DBDValueHandler target;

    public ProxyValueHandler(DBDValueHandler target) {
        this.target = target;
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return target.getValueObjectType(attribute);
    }

    @Nullable
    @Override
    public String getValueContentType(@NotNull DBSTypedObject attribute) {
        return target.getValueContentType(attribute);
    }

    @Nullable
    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        return target.fetchValueObject(session, resultSet, type, index);
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        target.bindValueObject(session, statement, type, index, value);
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy) throws DBCException {
        return target.getValueFromObject(session, type, object, copy);
    }

    @Override
    public void releaseValueObject(@Nullable Object value) {
        target.releaseValueObject(value);
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(@NotNull DBSTypedObject attribute) {
        return target.getSupportedOperators(attribute);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
        return target.getValueDisplayString(column, value, format);
    }
}