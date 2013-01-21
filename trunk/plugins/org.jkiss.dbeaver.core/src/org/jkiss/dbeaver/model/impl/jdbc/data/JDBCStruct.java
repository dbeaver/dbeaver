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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Struct holder
 */
public class JDBCStruct implements DBDStructure {

    static final Log log = LogFactory.getLog(JDBCStruct.class);

    private DBSDataType type;
    private Struct contents;
    private Map<DBSEntityAttribute, Object> values;

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
        try {
            return getValues(VoidProgressMonitor.INSTANCE).get(attribute);
        } catch (SQLException e) {
            throw new DBCException("SQL error while getting attributes", e);
        }
    }

    private Map<DBSEntityAttribute, Object> getValues(DBRProgressMonitor monitor) throws SQLException, DBCException
    {
        if (values == null) {
            DBSEntity entity = getStructType();
            if (entity == null) {
                throw new DBCException("Non-structure record '" + getTypeName() + "' doesn't have attributes");
            }

            values = new IdentityHashMap<DBSEntityAttribute, Object>();
            if (contents == null) {
                return values;
            }
            Object[] attrValues = contents.getAttributes();
            if (attrValues == null) {
                return values;
            }

            try {
                Collection<? extends DBSEntityAttribute> entityAttributes = entity.getAttributes(monitor);
                if (entityAttributes.size() != attrValues.length) {
                    log.warn("Number of entity attributes (" + entityAttributes.size() + ") differs from real values (" + attrValues.length + ")");
                }
                for (DBSEntityAttribute attr : entityAttributes) {
                    int ordinalPosition = attr.getOrdinalPosition() - 1;
                    if (ordinalPosition < 0 || ordinalPosition >= attrValues.length) {
                        log.warn("Attribute '" + attr.getName() + "' ordinal position (" + ordinalPosition + ") is out of range (" + attrValues.length + ")");
                        continue;
                    }
                    Object value = attrValues[ordinalPosition];
                    if (value instanceof Struct) {
                        Struct structValue = (Struct)value;
                        DBSDataType dataType;
                        try {
                            dataType = DBUtils.resolveDataType(monitor, entity.getDataSource(), structValue.getSQLTypeName());
                            if (dataType != null) {
                                value = new JDBCStruct(dataType, structValue);
                            }
                        } catch (DBException e) {
                            log.error("Error resolving data type '" + structValue.getSQLTypeName() + "'", e);
                        }
                    }
                    values.put(attr, value);
                }
            } catch (DBException e) {
                throw new DBCException("Can't obtain attributes meta information", e);
            }
        }
        return values;
    }

    @Override
    public void setAttributeValue(DBRProgressMonitor monitor, DBSEntityAttribute attribute, Object value) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

}
