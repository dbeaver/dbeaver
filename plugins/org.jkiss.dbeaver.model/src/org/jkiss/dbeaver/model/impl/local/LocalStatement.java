/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalResultSet
 */
public class LocalStatement implements DBCStatement
{
    private final DBCSession session;
    private String text;
    private DBCExecutionSource source;

    public LocalStatement(DBCSession session, String text) {
        this.session = session;
        this.text = text;
    }

    @Override
    public DBCSession getSession() {
        return session;
    }

    @Override
    public String getQueryString() {
        return text;
    }

    @Override
    public DBCExecutionSource getStatementSource() {
        return source;
    }

    @Override
    public void setStatementSource(DBCExecutionSource source) {
        this.source = source;
    }

    @Override
    public boolean executeStatement() throws DBCException {
        return false;
    }

    @Override
    public void addToBatch() throws DBCException {

    }

    @Override
    public int[] executeStatementBatch() throws DBCException {
        return new int[0];
    }

    @Override
    public DBCResultSet openResultSet() throws DBCException {
        return new LocalResultSet<>(session, this);
    }

    @Override
    public DBCResultSet openGeneratedKeysResultSet() throws DBCException {
        return null;
    }

    @Override
    public int getUpdateRowCount() throws DBCException {
        return 0;
    }

    @Override
    public boolean nextResults() throws DBCException {
        return false;
    }

    @Override
    public void setLimit(long offset, long limit) throws DBCException {

    }

    @Override
    public Throwable[] getStatementWarnings() throws DBCException {
        return new Throwable[0];
    }

    @Override
    public void setStatementTimeout(int timeout) throws DBCException {

    }

    @Override
    public void close() {

    }

    @Override
    public void cancelBlock(DBRProgressMonitor monitor, Thread blockThread) throws DBException {

    }
}
