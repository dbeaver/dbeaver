/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.sql.task;

import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.io.Writer;

/**
 * SQLScriptDataReceiver
 */
public class SQLScriptDataReceiver implements DBDDataReceiver {

    private Writer dumpWriter;

    @Override
    public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {

    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {

    }

    @Override
    public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {

    }

    @Override
    public void close() {

    }

    public void setDumpWriter(Writer writer) {
        this.dumpWriter = writer;
    }

    public Writer getDumpWriter() {
        return dumpWriter;
    }
}
