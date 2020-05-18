/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.sql.RowId;

/**
 * Row ID
 */
public class JDBCRowId implements DBDValue {

    private static final Log log = Log.getLog(JDBCRowId.class);

    private RowId value;

    public JDBCRowId(RowId value)
    {
        this.value = value;
    }

    public RowId getValue() throws DBCException
    {
        return value;
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull()
    {
        return value == null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release()
    {
        value = null;
    }

    @Override
    public String toString()
    {
        if (value == null) {
            return "null";
        }
        return new String(value.getBytes());
    }
}
