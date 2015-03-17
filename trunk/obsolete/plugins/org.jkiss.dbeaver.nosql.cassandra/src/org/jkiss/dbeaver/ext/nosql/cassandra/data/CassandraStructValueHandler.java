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
package org.jkiss.dbeaver.ext.nosql.cassandra.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCStructDynamic;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCStructStatic;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSetMetaData;
import java.sql.Struct;

/**
 * Object type support
 */
public class CassandraStructValueHandler extends JDBCStructValueHandler {

    public static final CassandraStructValueHandler INSTANCE = new CassandraStructValueHandler();

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new JDBCStructDynamic(session, null);
        } else if (object instanceof JDBCStructStatic) {
            return copy ? ((JDBCStructStatic) object).cloneValue(session.getProgressMonitor()) : object;
        } else if (object instanceof Struct) {
            // Obtain metadata information from struct
            ResultSetMetaData metaData = null;
            try {
                metaData = (ResultSetMetaData) object.getClass().getMethod("getMetaData").invoke(object);
            } catch (Throwable e) {
                // No metadata, use as plain value
            }
            return new JDBCStructDynamic(session, (Struct) object, metaData);
        } else {
            throw new DBCException("Unsupported struct type: " + object.getClass().getName());
        }
    }


}
