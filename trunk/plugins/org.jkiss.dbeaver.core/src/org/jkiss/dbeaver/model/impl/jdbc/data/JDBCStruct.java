/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

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

    @NotNull
    private DBSDataType type;
    @Nullable
    private Struct contents;
    @NotNull
    private Map<DBSAttributeBase, Object> values;
    @Nullable
    private Object structData;

    private JDBCStruct()
    {
    }

    public JDBCStruct(DBCSession session, @NotNull DBSDataType type, @Nullable Struct contents) throws DBCException
    {
        this(session, type, contents, null);
    }

    public JDBCStruct(DBCSession session, @NotNull DBSDataType type, @Nullable Object data) throws DBCException
    {
        this(session, type, null, null);
        this.structData = data;
    }

    public JDBCStruct(DBCSession session, @NotNull DBSDataType type, @Nullable Struct contents, @Nullable ResultSetMetaData metaData) throws DBCException
    {
        this.type = type;
        this.contents = contents;

        // Extract structure data
        values = new LinkedHashMap<DBSAttributeBase, Object>();
        try {
            Object[] attrValues = contents == null ? null : contents.getAttributes();
            if (type instanceof DBSEntity) {
                DBSEntity entity = (DBSEntity)type;
                Collection<? extends DBSEntityAttribute> entityAttributes = CommonUtils.safeCollection(entity.getAttributes(session.getProgressMonitor()));
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
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(session, attr);
                    value = valueHandler.getValueFromObject(session, attr, value, false);
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
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false);
                        values.put(attr, value);
                    }
                } else {
                    log.warn("Data type '" + contents.getSQLTypeName() + "' isn't resolved as structured type. Use synthetic attributes.");
                    for (int i = 0, attrValuesLength = attrValues.length; i < attrValuesLength; i++) {
                        Object value = attrValues[i];
                        DBSAttributeBase attr = new StructAttribute(i, value);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false);
                        values.put(attr, value);
                    }
                }
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Nullable
    public Struct getValue() throws DBCException
    {
        return contents;
    }

    @Override
    public boolean isNull()
    {
        return contents == null && structData == null;
    }

    @Override
    public void release()
    {
        contents = null;
        values.clear();
    }

    @NotNull
    public String getTypeName()
    {
        return type.getTypeName();
    }

    public String getStringRepresentation()
    {
        if (structData != null) {
            return String.valueOf(structData);
        }
        return getTypeName();
    }

/*
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
*/

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
    public Object getAttributeValue(@NotNull DBSAttributeBase attribute) throws DBCException
    {
        return values.get(attribute);
    }

    @Override
    public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) throws DBCException
    {
        values.put(attribute, value);
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
        DBPDataKind dataKind;
        public StructAttribute(int index, Object value)
        {
            if (value instanceof CharSequence) {
                dataKind = DBPDataKind.STRING;
                setValueType(Types.VARCHAR);
            } else if (value instanceof Number) {
                dataKind = DBPDataKind.NUMERIC;
                setValueType(Types.NUMERIC);
            } else if (value instanceof Boolean) {
                dataKind = DBPDataKind.BOOLEAN;
                setValueType(Types.BOOLEAN);
            } else if (value instanceof Date) {
                dataKind = DBPDataKind.DATETIME;
                setValueType(Types.TIMESTAMP);
            } else if (value instanceof byte[]) {
                dataKind = DBPDataKind.BINARY;
                setValueType(Types.BINARY);
            } else {
                dataKind = DBPDataKind.OBJECT;
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
                metaData.isNullable(index) == ResultSetMetaData.columnNoNulls,
                metaData.isAutoIncrement(index));
            dataKind = JDBCUtils.resolveDataKind(dataSource, getTypeName(), getTypeID());
        }

        @Override
        public DBPDataKind getDataKind()
        {
            return dataKind;
        }
    }
}
