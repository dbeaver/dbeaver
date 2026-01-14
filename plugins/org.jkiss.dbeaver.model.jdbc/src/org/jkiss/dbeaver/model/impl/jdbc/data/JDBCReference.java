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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDReference;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.Ref;
import java.sql.SQLException;

/**
 * Reference holder
 */
public class JDBCReference implements DBDReference {

    private static final Log log = Log.getLog(JDBCReference.class);

    private DBSDataType type;
    private Object value;
    private Object refObject;

    public JDBCReference(DBSDataType type, Object value) throws DBCException {
        this.type = type;
        this.value = value;
    }

    public Object getValue() throws DBCException {
        return value;
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release() {
        type = null;
        value = null;
    }

    @Override
    public DBSDataType getReferencedType() {
        return type;
    }

    @Override
    public Object getReferencedObject(DBCSession session) throws DBCException {
        if (refObject == null && value instanceof Ref ref) {
            try {
                DBRProgressMonitor monitor = session.getProgressMonitor();
                monitor.subTask("Read reference '" + type.getName() + "'");
                Object refValue = ref.getObject();
                DBDValueHandler valueHandler = DBUtils.findValueHandler(session, type);
                refObject = valueHandler.getValueFromObject(session, type, refValue, false, false);
            } catch (SQLException e) {
                throw new DBCException("Can't obtain object reference");
            }
        }
        return refObject;
    }

    @Override
    public String toString() {
        try {
            return value == null ? DBConstants.NULL_VALUE_LABEL :
                value instanceof Ref ? ((Ref) value).getBaseTypeName() : value.toString();
        } catch (SQLException e) {
            return value.toString();
        }
    }
}
