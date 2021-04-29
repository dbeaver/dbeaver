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

package org.jkiss.dbeaver.model.impl.local;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.AbstractStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * LocalResultSet
 */
public class LocalStatement extends AbstractStatement<DBCSession>
{
    private String text;

    public LocalStatement(DBCSession session, String text) {
        super(session);
        this.text = text;
    }

    @Override
    public String getQueryString() {
        return text;
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
        return new LocalResultSet<>(connection, this);
    }

    @Override
    public DBCResultSet openGeneratedKeysResultSet() throws DBCException {
        return null;
    }

    @Override
    public long getUpdateRowCount() throws DBCException {
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
    public void setResultsFetchSize(int fetchSize) throws DBCException {

    }

    @Override
    public void close() {

    }

    @Override
    public void cancelBlock(DBRProgressMonitor monitor, Thread blockThread) throws DBException {

    }
}
