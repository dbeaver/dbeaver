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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Stream session
 */
public class StreamTransferSession extends AbstractSession {

    private static final Log log = Log.getLog(StreamTransferSession.class);

    public StreamTransferSession(DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle) {
        super(monitor, purpose, taskTitle);
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return null;
    }

    @Override
    public DBPDataSource getDataSource() {
        return null;
    }

    @Override
    public DBCStatement prepareStatement(DBCStatementType type, String query, boolean scrollable, boolean updatable, boolean returnGeneratedKeys) throws DBCException {
        throw new DBCException("Not supported");
    }

    @Override
    public void cancelBlock(DBRProgressMonitor monitor, Thread blockThread) throws DBException {
        // do nothing
    }
}
