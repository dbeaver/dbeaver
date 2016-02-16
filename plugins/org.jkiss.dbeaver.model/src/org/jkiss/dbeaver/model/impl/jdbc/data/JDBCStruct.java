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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.sql.Connection;
import java.sql.Struct;

/**
 * abstract struct implementation.
 */
public abstract class JDBCStruct implements DBDStructure, DBDValueCloneable {

    static final Log log = Log.getLog(JDBCStruct.class);

    @NotNull
    protected DBSDataType type;
    @NotNull
    protected DBSEntityAttribute[] attributes;
    @NotNull
    protected Object[] values;

    @Override
    public boolean isNull()
    {
        for (Object value : values) {
            if (!DBUtils.isNullValue(value)) {
                return false;
            }
        }
        return true;
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
        return getTypeName();
    }

    @Override
    public DBSDataType getDataType()
    {
        return type;
    }

    @Override
    public Struct getRawValue() {
        Object[] attrs = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            Object attr = values[i];
            if (attr instanceof DBDValue) {
                attr = ((DBDValue) attr).getRawValue();
            }
            attrs[i] = attr;
        }
        final DBSDataType dataType = getDataType();
        try (DBCSession session = DBUtils.openUtilSession(VoidProgressMonitor.INSTANCE, dataType.getDataSource(), "Create struct")) {
            if (session instanceof Connection) {
                return ((Connection) session).createStruct(dataType.getTypeName(), attrs);
            } else {
                return new JDBCStructImpl(dataType.getTypeName(), attrs);
            }
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
    public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) throws DBCException
    {
        values[attribute.getOrdinalPosition()] = value;
    }

}
