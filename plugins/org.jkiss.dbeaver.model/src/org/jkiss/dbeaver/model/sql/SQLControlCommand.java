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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * SQL control command
 */
public class SQLControlCommand implements SQLScriptElement {

    private final DBPDataSource dataSource;
    private final String command;
    private final int offset;
    private final int length;
    private Object data;

    public SQLControlCommand(DBPDataSource dataSource, String command, int offset, int length) {
        this.dataSource = dataSource;
        this.command = command;
        this.offset = offset;
        this.length = length;
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public String getText() {
        return command;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }
}
