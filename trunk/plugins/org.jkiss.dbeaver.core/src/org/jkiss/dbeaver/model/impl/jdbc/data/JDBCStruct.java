/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.lang.ref.SoftReference;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Struct holder
 */
public class JDBCStruct implements DBDStructure, DBDValueCloneable {

    static final Log log = LogFactory.getLog(JDBCStruct.class);

    public static final int MAX_ITEMS_IN_STRING = 100;

    private DBSDataType type;
    private Struct contents;
    private Map<DBSAttributeBase, Object> values;
    private SoftReference<String> stringRepresentation;

    private JDBCStruct()
    {
    }

    public JDBCStruct(DBCExecutionContext context, DBSDataType type, Struct contents) throws DBCException
    {
        this(context, type, contents, null);
    }

    public JDBCStruct(DBCExecutionContext context, DBSDataType type, Struct contents, ResultSetMetaData metaData) throws DBCException
    {
        this.type = type;
        this.contents = contents;

        // Extract structure data
        values = new LinkedHashMap<DBSAttributeBase, Object>();
        try {
            Object[] attrValues = contents == null ? null : contents.getAttributes();
            if (type instanceof DBSEntity) {
                DBSEntity entity = (DBSEntity)type;
                Collection<? extends DBSEntityAttribute> entityAttributes = entity.getAttributes(context.getProgressMonitor());
                int valueCount = attrValues == null ? 0 : attrValues.length;
                if (attrValues != null && entityAttributes.size() != valueCount) {
                    log.warn("Number of entity attributes (" + entityAttributes.size() + ") differs from real values (" + valueCount + ")");
                }
                for (DBSEntityAttribute attr : entityAttributes) {
                    int ordinalPosition = attr.getOrdinalPosition() - 1;
                    if (ordinalPosition < 0 || attrValues != null && ordinalPosition >= valueCount) {
                        log.warn("Attribute '" + attr.getName() + "' ordinal position (" + ordinalPosition + ") is out of range (" + valueCount + ")");
                        continue;
                    }
                    Object value = attrValues != null ? attrValues[ordinalPosition] : null;
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(context, attr);
                    value = valueHandler.getValueFromObject(context, attr, value, false);
                    values.put(attr, value);
                }
            } else if (attrValues != null) {
                if (metaData != null) {
                    // Use meta data to read struct information
                    int attrCount = metaData.getColumnCount();
                    if (attrCount != attrValues.length) {
                        log.warn("Meta column count (" + attrCount + ") differs from value count (" + attrValues.length + ")");
                        attrCount = Math.min(attrCount, attrValues.length);
                    }
                    for (int i = 0; i < attrCount; i++) {
                        Object value = attrValues[i];
                        DBSAttributeBase attr = new StructAttribute(type.getDataSource(), metaData, i + 1);
                        value = DBUtils.findValueHandler(context, attr).getValueFromObject(context, attr, value, false);
                        values.put(attr, value);
                    }
                } else {
                    log.warn("Data type '" + contents.getSQLTypeName() + "' isn't resolved as structured type. Use synthetic attributes.");
                    for (int i = 0, attrValuesLength = attrValues.length; i < attrValuesLength; i++) {
                        Object value = attrValues[i];
                        DBSAttributeBase attr = new StructAttribute(i, value);
                        value = DBUtils.findValueHandler(context, attr).getValueFromObject(context, attr, value, false);
                        values.put(attr, value);
                    }
                }
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        } catch (SQLException e) {
            throw new DBCException("Can't read structure data", e);
        }
    }

    public Struct getValue() throws DBCException
    {
        return contents;
    }

    @Override
    public boolean isNull()
    {
        return contents == null || CommonUtils.isEmpty(values);
    }

    @Override
    public void release()
    {
        contents = null;
        values = null;
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
        String str = stringRepresentation != null ? stringRepresentation.get() : null;
        if (str == null) {
            try {
                str = makeStructString();
                stringRepresentation = new SoftReference<String>(str);
            } catch (SQLException e) {
                log.error(e);
                return contents.toString();
            }
        }
        return str;
    }

    private String makeStructString() throws SQLException
    {
        StringBuilder str = new StringBuilder(200);
        String typeName = contents.getSQLTypeName();
        if (typeName != null) {
            str.append(typeName);
        }
        str.append("(");
        int i = 0;
        for (Map.Entry<DBSAttributeBase, Object> entry : values.entrySet()) {
            Object item = entry.getValue();
            if (i > 0) str.append(",");
            //str.append(entry.getKey().getName()).append(':');
            if (DBUtils.isNullValue(item)) {
                str.append("NULL");
            } else {
                DBDValueHandler valueHandler = DBUtils.findValueHandler(type.getDataSource(), entry.getKey());
                String strValue = valueHandler.getValueDisplayString(entry.getKey(), item, DBDDisplayFormat.UI);
                SQLUtils.appendValue(str, entry.getKey(), strValue);
            }
            i++;
            if (i >= MAX_ITEMS_IN_STRING) {
                break;
            }
        }
        str.append(")");
        return str.toString();
    }

    @Override
    public DBSDataType getObjectDataType()
    {
        return type;
    }

    @Override
    public Collection<DBSAttributeBase> getAttributes()
    {
        return values.keySet();
    }

    @Override
    public Object getAttributeValue(DBSAttributeBase attribute) throws DBCException
    {
        return values.get(attribute);
    }

    @Override
    public void setAttributeValue(DBSAttributeBase attribute, Object value) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @Override
    public JDBCStruct cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        JDBCStruct copyStruct = new JDBCStruct();
        copyStruct.type = this.type;
        copyStruct.contents = null;
        copyStruct.values = new LinkedHashMap<DBSAttributeBase, Object>(this.values);
        return copyStruct;
    }

    private static class StructAttribute extends AbstractAttribute {
        DBSDataKind dataKind;
        public StructAttribute(int index, Object value)
        {
            if (value instanceof CharSequence) {
                dataKind = DBSDataKind.STRING;
                setValueType(Types.VARCHAR);
            } else if (value instanceof Number) {
                dataKind = DBSDataKind.NUMERIC;
                setValueType(Types.NUMERIC);
            } else if (value instanceof Boolean) {
                dataKind = DBSDataKind.BOOLEAN;
                setValueType(Types.BOOLEAN);
            } else if (value instanceof Date) {
                dataKind = DBSDataKind.DATETIME;
                setValueType(Types.TIMESTAMP);
            } else if (value instanceof byte[]) {
                dataKind = DBSDataKind.BINARY;
                setValueType(Types.BINARY);
            } else {
                dataKind = DBSDataKind.OBJECT;
                setValueType(Types.OTHER);
            }
            setName("Attr" + index);
            setOrdinalPosition(index);
            setTypeName(dataKind.name());
        }

        public StructAttribute(DBPDataSource dataSource, ResultSetMetaData metaData, int index) throws SQLException
        {
            super(
                metaData.getColumnName(index),
                metaData.getColumnTypeName(index),
                metaData.getColumnType(index),
                index,
                metaData.getColumnDisplaySize(index),
                metaData.getScale(index),
                metaData.getPrecision(index),
                false);
            dataKind = JDBCUtils.resolveDataKind(dataSource, getTypeName(), getTypeID());
        }

        @Override
        public DBSDataKind getDataKind()
        {
            return dataKind;
        }
    }
}
