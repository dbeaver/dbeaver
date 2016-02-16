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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.impl.struct.AbstractStructDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * Dynamic struct. Self contained entity.
 */
public class JDBCStructDynamic extends JDBCStruct {

    //public static final int MAX_ITEMS_IN_STRING = 100;

    @NotNull
    private DBPDataSource dataSource;

    private JDBCStructDynamic()
    {
    }

    public JDBCStructDynamic(DBCSession session, @Nullable Struct contents, @Nullable ResultSetMetaData metaData) throws DBCException
    {
        this.dataSource = session.getDataSource();
        this.type = new StructType();

        // Extract structure data
        try {
            Object[] attrValues = contents == null ? null : contents.getAttributes();
            if (attrValues != null) {
                attributes = new DBSEntityAttribute[attrValues.length];
                values = new Object[attrValues.length];
                if (metaData != null) {
                    // Use meta data to read struct information
                    int attrCount = metaData.getColumnCount();
                    if (attrCount != attrValues.length) {
                        log.warn("Meta column count (" + attrCount + ") differs from value count (" + attrValues.length + ")");
                        attrCount = Math.min(attrCount, attrValues.length);
                    }
                    for (int i = 0; i < attrCount; i++) {
                        Object value = attrValues[i];
                        StructAttribute attr = new StructAttribute(metaData, i);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false);
                        attributes[i] = attr;
                        values[i] = value;
                    }
                } else {
                    log.warn("Data type '" + contents.getSQLTypeName() + "' isn't resolved as structured type. Use synthetic attributes.");
                    for (int i = 0, attrValuesLength = attrValues.length; i < attrValuesLength; i++) {
                        Object value = attrValues[i];
                        StructAttribute attr = new StructAttribute(i, value);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false);
                        attributes[i] = attr;
                        values[i] = value;
                    }
                }
            } else {
                this.attributes = EMPTY_ATTRIBUTE;
                this.values = EMPTY_VALUES;
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public DBSDataType getDataType()
    {
        return type;
    }

    @Override
    public JDBCStructDynamic cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        JDBCStructDynamic copyStruct = new JDBCStructDynamic();
        copyStruct.attributes = Arrays.copyOf(this.attributes, this.attributes.length);
        copyStruct.values = Arrays.copyOf(this.values, this.values.length);
        return copyStruct;
    }

    private class StructType extends AbstractStructDataType<DBPDataSource> implements DBSEntity {
        public StructType() {
            super(dataSource);
        }

        @NotNull
        @Override
        public String getTypeName() {
            return "Object";
        }

        @Override
        public int getTypeID() {
            return Types.STRUCT;
        }

        @Override
        public DBPDataKind getDataKind() {
            return DBPDataKind.STRUCT;
        }

        @NotNull
        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TYPE;
        }

        @Nullable
        @Override
        public Collection<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) {
            return Arrays.asList(attributes);
        }
    }

    private class StructAttribute extends AbstractAttribute implements DBSEntityAttribute {
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

        public StructAttribute(ResultSetMetaData metaData, int index) throws SQLException
        {
            super(
                metaData.getColumnName(index + 1),
                metaData.getColumnTypeName(index + 1),
                metaData.getColumnType(index + 1),
                index,
                metaData.getColumnDisplaySize(index + 1),
                metaData.getScale(index + 1),
                metaData.getPrecision(index + 1),
                metaData.isNullable(index + 1) == ResultSetMetaData.columnNoNulls,
                metaData.isAutoIncrement(index + 1));
            dataKind = JDBCUtils.resolveDataKind(dataSource, getTypeName(), getTypeID());
        }

        @Override
        public DBPDataKind getDataKind()
        {
            return dataKind;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StructAttribute)) {
                return false;
            }
            StructAttribute attr = (StructAttribute)obj;
            return CommonUtils.equalObjects(name, attr.name) &&
                valueType == attr.valueType &&
                maxLength == attr.maxLength &&
                scale == attr.scale &&
                precision == attr.precision &&
                CommonUtils.equalObjects(typeName, attr.typeName) &&
                ordinalPosition == attr.ordinalPosition;
        }

        @Override
        public int hashCode() {
            return (int) (name.hashCode() + valueType + maxLength + scale + precision + typeName.hashCode() + ordinalPosition);
        }

        @Nullable
        @Override
        public String getDefaultValue() {
            return null;
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return (StructType) type;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return dataSource;
        }
    }

    /*
        private String makeStructString() throws SQLException
        {
            StringBuilder str = new StringBuilder(200);
            String typeName = getTypeName();
            str.append(typeName);
            str.append("(");
            int i = 0;
            for (int i1 = 0; i1 < attributes.length; i1++) {
                DBSEntityAttribute attr = attributes[i1];
                Object item = values[i];
                if (i > 0) str.append(",");
                //str.append(entry.getKey().getName()).append(':');
                if (DBUtils.isNullValue(item)) {
                    str.append("NULL");
                } else {
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(dataSource, attr);
                    String strValue = valueHandler.getValueDisplayString(attr, item, DBDDisplayFormat.UI);
                    SQLUtils.appendValue(str, attr, strValue);
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

}
