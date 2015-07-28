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
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple objects cache.
 */
public final class JDBCObjectSimpleCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends JDBCObjectCache<OWNER, OBJECT> {
    private final String query;
    private final Class<OBJECT> objectType;
    private final Object[] queryParameters;
    private Constructor<OBJECT> objectConstructor;

    public JDBCObjectSimpleCache(Class<OBJECT> objectType, String query, Object ... args)
    {
        this.query = query;
        this.objectType = objectType;
        this.queryParameters = args;
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException
    {
        JDBCPreparedStatement dbStat = session.prepareStatement(query);
        if (queryParameters != null) {
            for (int i = 0; i < queryParameters.length; i++) {
                dbStat.setObject(i + 1, queryParameters[i]);
            }
        }
        return dbStat;
    }

    @Override
    protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull ResultSet resultSet)
        throws SQLException, DBException
    {
        try {
            if (objectConstructor == null) {
                for (Class<?> argType = owner.getClass(); argType != null; argType = argType.getSuperclass()) {
                    try {
                        objectConstructor = objectType.getConstructor(argType, ResultSet.class);
                        break;
                    } catch (Exception e) {
                        // Not found - check interfaces
                        for (Class<?> intType : argType.getInterfaces()) {
                            try {
                                objectConstructor = objectType.getConstructor(intType, ResultSet.class);
                                break;
                            } catch (Exception e2) {
                                // Not found
                            }
                        }
                        if (objectConstructor != null) {
                            break;
                        }
                    }
                }
                if (objectConstructor == null) {
                    throw new DBException("Can't find proper constructor for object '" + objectType.getName() + "'");
                }
            }
            return objectConstructor.newInstance(owner, resultSet);
        } catch (Exception e) {
            throw new DBException(
                "Error creating cache object",
                e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e);
        }
    }

}
