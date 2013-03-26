/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML type support
 */
public class OracleXMLValueHandler extends JDBCContentValueHandler {

    public static final OracleXMLValueHandler INSTANCE = new OracleXMLValueHandler();

    @Override
    protected DBDContent fetchColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException
    {
        Object object;

        try {
            object = resultSet.getObject(index);
        } catch (SQLException e) {
            try {
                object = resultSet.getSQLXML(index);
            } catch (SQLException e1) {
/*
                try {
                    ResultSet originalRS = resultSet.getOriginal();
                    Class<?> rsClass = originalRS.getClass().getClassLoader().loadClass("oracle.jdbc.OracleResultSet");
                    Method method = rsClass.getMethod("getOPAQUE", Integer.TYPE);
                    object = method.invoke(originalRS, index);
                    if (object != null) {
                        Class<?> xmlType = object.getClass().getClassLoader().loadClass("oracle.xdb.XMLType");
                        Method xmlConstructor = xmlType.getMethod("createXML", object.getClass());
                        object = xmlConstructor.invoke(null, object);
                    }
                }
                catch (Throwable e2) {
                    object = null;
                }
*/
                object = null;
            }
        }

        if (object == null) {
            return new OracleContentXML(context.getDataSource(), null);
        } else if (object.getClass().getName().equals("oracle.xdb.XMLType")) {
            return new OracleContentXML(context.getDataSource(), new OracleXMLWrapper(object));
        } else if (object instanceof SQLXML) {
            return new OracleContentXML(context.getDataSource(), (SQLXML) object);
        } else {
            throw new DBCException("Unsupported object type: " + object.getClass().getName());
        }
    }

    @Override
    public DBDContent getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new OracleContentXML(context.getDataSource(), null);
        }
        return super.getValueFromObject(context, type, object, copy);
    }

}
