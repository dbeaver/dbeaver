/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.tools;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ExasolJDBCObjectSimpleCacheLiterals<OWNER extends DBSObject, OBJECT extends DBSObject>
		extends JDBCObjectCache<OWNER, OBJECT> {

    private final String query;
    private final Class<OBJECT> objectType;
    private final Object[] queryParameters;
    private Constructor<OBJECT> objectConstructor;
    private String executeQuery = "";
	
	
    public ExasolJDBCObjectSimpleCacheLiterals(Class<OBJECT> objectType, String query, Object ... args)
    {
        this.query = query;
        this.objectType = objectType;
        this.queryParameters = args;
    }

    @NotNull
    @SuppressWarnings("rawtypes")
	@Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner)
        throws SQLException
    {
    	
        JDBCStatement dbStat = session.createStatement();
        
        //escape parameters
        for (int i = 0; i < queryParameters.length; i++) {
        	queryParameters[i] = (Object) ExasolUtils.quoteString(queryParameters[i].toString());
		}
        executeQuery = String.format(query, queryParameters);
        
        ((JDBCStatementImpl) dbStat).setQueryString(executeQuery);
        
        return dbStat;
    }
    
   

    @Override
    protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull JDBCResultSet resultSet)
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
