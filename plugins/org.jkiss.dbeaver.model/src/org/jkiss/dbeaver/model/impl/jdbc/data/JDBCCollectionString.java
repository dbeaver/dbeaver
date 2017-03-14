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
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

/**
 * Fake array holder
 */
public class JDBCCollectionString implements DBDCollection {

    private final DBSDataType type;
    private final DBDValueHandler valueHandler;
    private String value;

    public JDBCCollectionString(DBSDataType type, DBDValueHandler valueHandler, String value) {
        this.type = type;
        this.valueHandler = valueHandler;
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

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void release() {

    }

    @NotNull
    @Override
    public DBSDataType getComponentType() {
        return type;
    }

    @NotNull
    @Override
    public DBDValueHandler getComponentValueHandler() {
        return valueHandler;
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public Object getItem(int index) {
        return value;
    }

    @Override
    public void setItem(int index, Object value) {
        this.value = CommonUtils.toString(value);
    }

    @Override
    public void setContents(Object[] contents) {

    }

    @Override
    public String toString() {
        return value;
    }
}
