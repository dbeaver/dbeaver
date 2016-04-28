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

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.*;

/**
 * Static struct holder.
 * Attributes described by static data type.
 */
public class JDBCCompositeStatic extends JDBCComposite {

    private static final Log log = Log.getLog(JDBCCompositeStatic.class);

    public JDBCCompositeStatic(@NotNull JDBCComposite struct, @NotNull DBRProgressMonitor monitor) throws DBCException {
        super(struct, monitor);
    }

    public JDBCCompositeStatic(DBCSession session, @NotNull DBSDataType type, @Nullable Struct contents) throws DBCException
    {
        this.type = type;

        // Extract structure data
        try {
            Object[] attrValues = contents == null ? null : contents.getAttributes();
            if (type instanceof DBSEntity) {
                DBSEntity entity = (DBSEntity)type;
                Collection<? extends DBSEntityAttribute> entityAttributes = CommonUtils.safeCollection(entity.getAttributes(session.getProgressMonitor()));
                int valueCount = attrValues == null ? 0 : attrValues.length;
                if (attrValues != null && entityAttributes.size() != valueCount) {
                    log.warn("Number of entity attributes (" + entityAttributes.size() + ") differs from real values (" + valueCount + ")");
                }
                attributes = entityAttributes.toArray(new DBSEntityAttribute[entityAttributes.size()]);
                values = new Object[attributes.length];
                for (int i = 0; i < attributes.length; i++) {
                    DBSEntityAttribute attr = attributes[i];
                    int ordinalPosition = attr.getOrdinalPosition();
                    if (ordinalPosition < 0 || attrValues != null && ordinalPosition >= valueCount) {
                        log.warn("Attribute '" + attr.getName() + "' ordinal position (" + ordinalPosition + ") is out of range (" + valueCount + ")");
                        continue;
                    }
                    Object value = attrValues != null ? attrValues[ordinalPosition] : null;
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(session, attr);
                    value = valueHandler.getValueFromObject(session, attr, value, false);
                    values[ordinalPosition] = value;
                }
            } else {
                attributes = EMPTY_ATTRIBUTE;
                values = EMPTY_VALUES;
            }
        } catch (DBException e) {
            throw new DBCException("Can't obtain attributes meta information", e);
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public JDBCCompositeStatic cloneValue(DBRProgressMonitor monitor) throws DBCException
    {
        return new JDBCCompositeStatic(this, monitor);
    }

}
