/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * Fake array holder
 */
public class JDBCCollectionString extends JDBCCollection {

    private String value;

    JDBCCollectionString(DBSDataType type, DBDValueHandler valueHandler, String value) {
        super(type, valueHandler, new Object[] { value });
        this.value = value;
    }

    JDBCCollectionString(DBSDataType type, DBDValueHandler valueHandler, String value, Object[] contents) {
        super(type, valueHandler, contents);
        this.value = value;
    }

    @Override
    public Object getRawValue() {
        return value;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @NotNull
    public String makeArrayString(DBDDisplayFormat format) {
        if (isModified()) {
            return super.makeArrayString(format);
        }
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
