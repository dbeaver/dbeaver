/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.duckdb.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeMap;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;

import java.sql.Struct;
import java.util.Map;

public class DuckDBStructValueHandler extends JDBCStructValueHandler {

    public static final DuckDBStructValueHandler INSTANCE = new DuckDBStructValueHandler();

    private static final Log log = Log.getLog(DuckDBStructValueHandler.class);

    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object instanceof Struct) {
            try {
                Map<String, Object> map = getMap(object);
                return new JDBCCompositeMap(session, null, map);
            } catch (DBCException e) {
                log.warn(e);
            }
        } else {
            log.warn("Incorrect use of handler: " + this.getClass().getSimpleName());
        }

        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(@NotNull Object object) throws DBCException {
        try {
            return (Map<String, Object>) BeanUtils.invokeObjectDeclaredMethod(object, "getMap", new Class[0], new Object[0]);
        } catch (Throwable e) {
            throw new DBCException("Can't get structure as map", e);
        }
    }

}
