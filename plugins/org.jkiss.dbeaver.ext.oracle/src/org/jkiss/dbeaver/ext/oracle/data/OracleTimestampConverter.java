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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.utils.BeanUtils;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Calendar;

public class OracleTimestampConverter {
    // See  https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jajdb/oracle/sql/TIMESTAMP.html#timestampValue--
    private static final String TO_TIMESTAMP_METHOD_NAME = "timestampValue";

    private OracleTimestampConverter() {
    }

    public static Timestamp toTimestamp(@NotNull Object object, @NotNull Connection connection) throws Exception {
        Class<?> aClass = object.getClass();
        return switch (aClass.getName()) {
        case OracleConstants.TIMESTAMP_CLASS_NAME ->
            (Timestamp) invokeNativeMethod(object, TO_TIMESTAMP_METHOD_NAME, null, null);
        case OracleConstants.TIMESTAMPTZ_CLASS_NAME -> (Timestamp) invokeNativeMethod(object, TO_TIMESTAMP_METHOD_NAME,
                new Class<?>[] { Connection.class }, new Object[] { connection });
        case OracleConstants.TIMESTAMPLTZ_CLASS_NAME -> (Timestamp) invokeNativeMethod(object, TO_TIMESTAMP_METHOD_NAME,
                new Class<?>[] { Connection.class, Calendar.class },
                new Object[] { connection, Calendar.getInstance() });
        default -> throw new DBException("Unsupported Oracle TIMESTAMP type: " + aClass.getName());
        };
    }

    private static Object invokeNativeMethod(Object object, String name, Class<?>[] classes, Object[] args) throws Exception {
        try {
            return BeanUtils.invokeObjectMethod(object, name, classes, args);
        } catch (Throwable e) {
            throw new DBException("Cannot invoke method " + name + " on " + object.getClass(), e);
        }
    }
}
