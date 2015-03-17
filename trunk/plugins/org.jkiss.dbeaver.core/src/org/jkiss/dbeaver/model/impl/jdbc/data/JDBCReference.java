/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDReference;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Ref;
import java.sql.SQLException;

/**
 * Reference holder
 */
public class JDBCReference implements DBDReference {

    static final Log log = Log.getLog(JDBCReference.class);

    private DBSDataType type;
    private Ref value;
    private Object refObject;

    public JDBCReference(DBSDataType type, Ref value) throws DBCException
    {
        this.type = type;
        this.value = value;
    }

    public Ref getValue() throws DBCException
    {
        return value;
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull()
    {
        return value == null;
    }

    @Override
    public void release()
    {
        type = null;
        value = null;
    }

    @Override
    public DBSDataType getReferencedType()
    {
        return type;
    }

    @Override
    public Object getReferencedObject(DBCSession session) throws DBCException
    {
        if (refObject == null) {
            try {
                session.getProgressMonitor().beginTask("Retrieve references object", 3);
                try {
                    session.getProgressMonitor().worked(1);
                    Object refValue = value.getObject();
                    session.getProgressMonitor().worked(1);
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(session, type);
                    refObject = valueHandler.getValueFromObject(session, type, refValue, false);
                    session.getProgressMonitor().worked(1);
                } finally {
                    session.getProgressMonitor().done();
                }
            } catch (SQLException e) {
                throw new DBCException("Can't obtain object reference");
            }
        }
        return refObject;
    }

    @Override
    public String toString()
    {
        try {
            return value == null ? DBConstants.NULL_VALUE_LABEL : value.getBaseTypeName();
        } catch (SQLException e) {
            return value.toString();
        }
    }
}
