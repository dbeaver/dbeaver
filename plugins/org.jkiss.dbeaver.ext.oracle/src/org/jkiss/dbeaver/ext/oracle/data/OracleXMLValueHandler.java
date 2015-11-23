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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML type support
 */
public class OracleXMLValueHandler extends JDBCContentValueHandler {

    public static final OracleXMLValueHandler INSTANCE = new OracleXMLValueHandler();

    @NotNull
    @Override
    public String getValueContentType(@NotNull DBSTypedObject attribute) {
        return MimeTypes.TEXT_XML;
    }

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException, SQLException
    {
        Object object;

        //OracleResultSet oracleResultSet = (OracleResultSet) resultSet.getOriginal();
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
            return new OracleContentXML(session.getDataSource(), null);
        } else if (object.getClass().getName().equals(OracleConstants.XMLTYPE_CLASS_NAME)) {
            return new OracleContentXML(session.getDataSource(), new OracleXMLWrapper(object));
        } else if (object instanceof SQLXML) {
            return new OracleContentXML(session.getDataSource(), (SQLXML) object);
        } else {
            throw new DBCException("Unsupported object type: " + object.getClass().getName());
        }
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new OracleContentXML(session.getDataSource(), null);
        } else if (object instanceof OracleContentXML) {
            return copy ? (OracleContentXML)((OracleContentXML) object).cloneValue(session.getProgressMonitor()) : (OracleContentXML) object;
        }
        return super.getValueFromObject(session, type, object, copy);
    }

}
