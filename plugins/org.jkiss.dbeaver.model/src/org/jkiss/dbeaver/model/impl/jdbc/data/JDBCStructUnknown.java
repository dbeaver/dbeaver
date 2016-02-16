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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.Arrays;

/**
 * Unknown struct.
 */
public class JDBCStructUnknown extends JDBCStruct {

    public JDBCStructUnknown(@NotNull JDBCStruct struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        super(struct, monitor);
    }

    public JDBCStructUnknown(@NotNull DBCSession session, @Nullable Object structData)
    {
        this.type = new StructType(session.getDataSource());
        this.attributes = new DBSEntityAttribute[] { new StructAttribute(type, 0, structData) };
        this.values = new Object[] { structData };
    }

    @Override
    public JDBCStructUnknown cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        return new JDBCStructUnknown(this, monitor);
    }

    public String getStringRepresentation() {
        return String.valueOf(values[0]);
    }

}
