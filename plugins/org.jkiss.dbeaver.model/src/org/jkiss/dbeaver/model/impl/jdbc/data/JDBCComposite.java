/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.impl.struct.AbstractStructDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * abstract struct implementation.
 */
public abstract class JDBCComposite implements DBDComposite, DBDValueCloneable {

    private static final Log log = Log.getLog(JDBCComposite.class);

    @Nullable
    private Struct rawStruct;

    @NotNull
    protected DBSDataType type;
    @NotNull
    protected DBSEntityAttribute[] attributes;
    @NotNull
    protected Object[] values;
    protected boolean modified;

    protected JDBCComposite() {
    }

    public JDBCComposite(Struct rawStruct) {
        this.rawStruct = rawStruct;
    }

    protected JDBCComposite(@NotNull JDBCComposite struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        this.type = struct.type;
        this.attributes = Arrays.copyOf(struct.attributes, struct.attributes.length);
        this.values = new Object[struct.values.length];
        for (int i = 0; i < struct.values.length; i++) {
            Object value = struct.values[i];
            if (value instanceof DBDValueCloneable) {
                value = ((DBDValueCloneable)value).cloneValue(monitor);
            }
            this.values[i] = value;
        }
    }

    @Override
    public boolean isNull()
    {
        if (ArrayUtils.isEmpty(values)) {
            return true;
        }
        for (Object value : values) {
            if (!DBUtils.isNullValue(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void release()
    {
        values = EMPTY_VALUES;
    }

    @NotNull
    public String getTypeName()
    {
        return type.getTypeName();
    }

    public String getStringRepresentation()
    {
        return CommonUtils.toString(getRawValue());
    }

    @NotNull
    public Object[] getValues() {
        return values;
    }

    public Struct getStructValue() throws DBCException {
        if (rawStruct != null) {
            return rawStruct;
        }
        Object[] attrs = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            Object attr = values[i];
            if (attr instanceof DBDValue) {
                attr = ((DBDValue) attr).getRawValue();
            }
            attrs[i] = attr;
        }
        final DBSDataType dataType = getDataType();
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataType, "Create JDBC struct")) {
            if (session instanceof Connection) {
                return ((Connection) session).createStruct(dataType.getTypeName(), attrs);
            } else {
                return new JDBCStructImpl(dataType.getTypeName(), attrs, getStringRepresentation());
            }
        } catch (Throwable e) {
            throw new DBCException("Error creating struct", e);
        }
    }

    @Override
    public DBSDataType getDataType()
    {
        return type;
    }

    @Override
    public Struct getRawValue() {
        if (rawStruct != null) {
            return rawStruct;
        }
        try {
            return getStructValue();
        } catch (Throwable e) {
            log.error(e);
            return null;
        }
    }

    @NotNull
    @Override
    public DBSAttributeBase[] getAttributes()
    {
        return attributes;
    }

    @Nullable
    @Override
    public Object getAttributeValue(@NotNull DBSAttributeBase attribute) throws DBCException
    {
        int position = attribute.getOrdinalPosition();
        if (position >= values.length) {
            log.debug("Attribute index is out of range (" + position + ">=" + values.length + ")");
            return null;
        }
        return values[position];
    }

    @Override
    public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) {
        if (!CommonUtils.equalObjects(values[attribute.getOrdinalPosition()], value)) {
            this.values[attribute.getOrdinalPosition()] = value;
            this.modified = true;
        }
    }

    @Override
    public String toString() {
        return getStringRepresentation();
    }

    protected class StructType extends AbstractStructDataType<DBPDataSource> implements DBSEntity {
        public StructType(DBPDataSource dataSource) {
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

    protected static class StructAttribute extends AbstractAttribute implements DBSEntityAttribute {
        final DBSDataType type;
        DBPDataKind dataKind;
        public StructAttribute(DBSDataType type, int index, Object value)
        {
            this.type = type;
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

        public StructAttribute(DBSDataType type, ResultSetMetaData metaData, int index) throws SQLException
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
            this.type = type;
            dataKind = JDBCUtils.resolveDataKind(type.getDataSource(), getTypeName(), getTypeID());
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
                CommonUtils.equalObjects(scale, attr.scale) &&
                CommonUtils.equalObjects(precision, attr.precision) &&
                CommonUtils.equalObjects(typeName, attr.typeName) &&
                ordinalPosition == attr.ordinalPosition;
        }

        @Override
        public int hashCode() {
            return (int) ((name == null ? 0 : name.hashCode()) +
                valueType + maxLength + CommonUtils.toInt(scale) + CommonUtils.toInt(precision) +
                (typeName == null ? 0 : typeName.hashCode()) +
                ordinalPosition);
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
            return type.getDataSource();
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
