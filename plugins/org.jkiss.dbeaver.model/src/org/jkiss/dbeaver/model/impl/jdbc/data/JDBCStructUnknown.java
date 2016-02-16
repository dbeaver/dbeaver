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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Arrays;

/**
 * Unknown struct.
 */
public class JDBCStructUnknown extends JDBCStruct {

    @Nullable
    private Object structData;

    private JDBCStructUnknown()
    {
    }

    public JDBCStructUnknown(@Nullable Object structData)
    {
        this.structData = structData;
        this.attributes = EMPTY_ATTRIBUTE;
        this.values = EMPTY_VALUES;
    }

    @Override
    public JDBCStructUnknown cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        JDBCStructUnknown copyStruct = new JDBCStructUnknown();
        copyStruct.structData = structData;
        copyStruct.attributes = Arrays.copyOf(this.attributes, this.attributes.length);
        copyStruct.values = Arrays.copyOf(this.values, this.values.length);
        return copyStruct;
    }

    @Override
    public void release()
    {
        structData = null;
    }

    public String getStringRepresentation() {
        return String.valueOf(structData);
    }

}
