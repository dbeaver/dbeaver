/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.sql.SQLException;
import java.sql.Struct;

/**
 * Struct holder
 */
public class JDBCStruct implements DBDStructure {

    static final Log log = LogFactory.getLog(JDBCStruct.class);

    private DBSDataType type;
    private Struct contents;

    public JDBCStruct(DBSDataType type, Struct contents)
    {
        this.type = type;
        this.contents = contents;
    }

    public Struct getValue() throws DBCException
    {
        return contents;
    }

    @Override
    public boolean isNull()
    {
        return contents == null;
    }

    @Override
    public DBDValue makeNull()
    {
        return new JDBCStruct(type, null);
    }

    @Override
    public void release()
    {
    }

    public String getTypeName()
    {
        try {
            return contents == null ? null : contents.getSQLTypeName();
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    public String getStringRepresentation()
    {
        try {
            return makeStructString(contents);
        } catch (SQLException e) {
            log.error(e);
            return contents.toString();
        }
    }

    private static String makeStructString(Struct contents) throws SQLException
    {
        if (contents == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        StringBuilder str = new StringBuilder(200);
        String typeName = contents.getSQLTypeName();
        if (typeName != null) {
            str.append(typeName);
        }
        str.append("(");
        final Object[] attributes = contents.getAttributes();
        for (int i = 0, attributesLength = attributes.length; i < attributesLength; i++) {
            Object item = attributes[i];
            if (item == null) {
                continue;
            }
            if (i > 0) str.append(',');
            str.append('\'');
            if (item instanceof Struct) {
                // Nested structure
                str.append(makeStructString((Struct) item));
            } else {
                // Childish, but we can't use anything but toString
                str.append(item.toString());
            }
            str.append('\'');
        }
        str.append(")");
        return str.toString();
    }

    @Override
    public DBSEntity getStructType()
    {
        return type instanceof DBSEntity ? (DBSEntity) type : null;
    }

    @Override
    public Object getAttributeValue(DBRProgressMonitor monitor, DBSEntityAttribute attribute) throws DBCException
    {
        DBSEntity entity = getStructType();
        if (entity == null) {
            throw new DBCException("Non-structure record '" + getTypeName() + "' doesn't have attributes");
        }
        int index = -1, i = 0;
        try {
            for (DBSEntityAttribute attr : entity.getAttributes(monitor)) {
                if (attr == attribute) {
                    index = i;
                }
                i++;
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        }
        if (index < 0) {
            throw new DBCException("Attribute '" + attribute.getName() + "' doesn't belong to structure type '" + getTypeName() + "'");
        }
        try {
            Object[] values = contents.getAttributes();
            if (index >= values.length) {
                throw new DBCException("Attribute index is out of range (" + index + ">=" + values.length + ")");
            }
            return values[i];
        } catch (SQLException e) {
            throw new DBCException("Error getting structure attribute values", e);
        }
    }

    @Override
    public void setAttributeValue(DBRProgressMonitor monitor, DBSEntityAttribute attribute, Object value) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

}
