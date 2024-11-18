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
package org.jkiss.dbeaver.ext.kingbase.model.data;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * KingbaseJSONValueHandler
 */
public class KingbaseJSONValueHandler extends JDBCContentValueHandler {

    public static final KingbaseJSONValueHandler INSTANCE = new KingbaseJSONValueHandler();

    @Override
    protected DBDContent fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index) throws SQLException {
        String json = resultSet.getString(index);
        return new KingbaseContentJSON(session.getExecutionContext(), json);
    }

    @Override
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        if (KingbaseUtils.isKBObject(object)) {
            object = KingbaseUtils.extractKBObjectValue(object);
        }
        if (object == null) {
            return new KingbaseContentJSON(session.getExecutionContext(), null);
        } else if (object instanceof KingbaseContentJSON) {
            return copy ? ((KingbaseContentJSON) object).cloneValue(session.getProgressMonitor()) : (KingbaseContentJSON) object;
        } else if (object instanceof String) {
            return new KingbaseContentJSON(session.getExecutionContext(), (String) object);
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }
}
