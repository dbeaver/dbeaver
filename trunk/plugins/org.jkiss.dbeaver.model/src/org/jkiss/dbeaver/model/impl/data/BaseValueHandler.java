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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Base value handler
 */
public abstract class BaseValueHandler implements DBDValueHandler {

    @NotNull
    @Override
    public String getValueContentType(@NotNull DBSTypedObject attribute) {
        return null;
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        return DBUtils.getDefaultValueDisplayString(value, format);
    }


    @Override
    public DBCLogicalOperator[] getSupportedOperators(@NotNull DBDAttributeBinding attribute) {
        return DBUtils.getDefaultOperators(attribute);
    }

}