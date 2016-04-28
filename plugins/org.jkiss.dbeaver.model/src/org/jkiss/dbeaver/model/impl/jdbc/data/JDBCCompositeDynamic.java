/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;

/**
 * Dynamic struct. Self contained entity.
 */
public class JDBCCompositeDynamic extends JDBCComposite {

    private static final Log log = Log.getLog(JDBCCompositeDynamic.class);
        //public static final int MAX_ITEMS_IN_STRING = 100;


    public JDBCCompositeDynamic(@NotNull JDBCComposite struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        super(struct, monitor);
    }

    public JDBCCompositeDynamic(@NotNull DBCSession session, @Nullable Struct contents, @Nullable ResultSetMetaData metaData) throws DBCException
    {
        this.type = new StructType(session.getDataSource());

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
                        StructAttribute attr = new StructAttribute(this.type, metaData, i);
                        value = DBUtils.findValueHandler(session, attr).getValueFromObject(session, attr, value, false);
                        attributes[i] = attr;
                        values[i] = value;
                    }
                } else {
                    log.warn("Data type '" + contents.getSQLTypeName() + "' isn't resolved as structured type. Use synthetic attributes.");
                    for (int i = 0, attrValuesLength = attrValues.length; i < attrValuesLength; i++) {
                        Object value = attrValues[i];
                        StructAttribute attr = new StructAttribute(this.type, i, value);
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
    public JDBCCompositeDynamic cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        return new JDBCCompositeDynamic(this, monitor);
    }

}
