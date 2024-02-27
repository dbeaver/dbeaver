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
package org.jkiss.dbeaver.ext.cubrid.model.plan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;

public class CubridStatementProxy 
{

    protected Statement statement;

    public CubridStatementProxy(Statement statement) 
    {
        this.statement = statement;
    }

    public String getQueryplan(String sql) throws SQLException 
    {
        return (String) invoke(statement, "getQueryplan", String.class, sql);
    }

    private static Object invoke(Object objSrc, String methodName, Class<?> clazz, Object obj)
            throws SQLException 
    {
        try {
            Method m = objSrc.getClass().getMethod(methodName, new Class<?>[] {clazz});
            return m.invoke(objSrc, new Object[] {obj});
        } catch (SecurityException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw new SQLException(e.getMessage(), null, -90000);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw new SQLException(e.getMessage(), null, -90001);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SQLException) {
                throw new SQLException(e.getMessage(), e.getTargetException());
            } else {
                throw new SQLException(
                        e.getMessage() + "\r\n" + e.getTargetException().getMessage(),
                        null,
                        -90002);
            }
        }
    }
}
