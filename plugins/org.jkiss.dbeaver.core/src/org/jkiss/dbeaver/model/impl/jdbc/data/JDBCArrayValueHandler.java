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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;

/**
 * JDBC Array value handler.
 * Handle ARRAY types.
 *
 * @author Serge Rider
 */
public class JDBCArrayValueHandler extends JDBCComplexValueHandler {

    static final Log log = Log.getLog(JDBCArrayValueHandler.class);

    public static final JDBCArrayValueHandler INSTANCE = new JDBCArrayValueHandler();

    @Override
    public Class getValueObjectType()
    {
        return JDBCArray.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return JDBCArray.makeArray((JDBCSession) session, null);
        } else if (object instanceof JDBCArray) {
            return copy ? ((JDBCArray) object).cloneValue(session.getProgressMonitor()) : object;
        } else if (object instanceof Array) {
            return JDBCArray.makeArray((JDBCSession) session, (Array)object);
        } else {
            throw new DBCException(CoreMessages.model_jdbc_exception_unsupported_array_type_ + object.getClass().getName());
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof JDBCArray) {
            String displayString = ((JDBCArray) value).makeArrayString();
            if (displayString != null) {
                return displayString;
            }
        }
        return super.getValueDisplayString(column, value, format);
    }


}