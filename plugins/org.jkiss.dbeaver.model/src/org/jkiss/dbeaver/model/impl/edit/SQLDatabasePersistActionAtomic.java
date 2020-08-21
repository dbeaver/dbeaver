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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;

/**
 * Atomic persist action. Executed in auto-commit mode
 */
public class SQLDatabasePersistActionAtomic extends SQLDatabasePersistAction {

    private boolean makeAtomic;
    private boolean wasTransactional = false;

    public SQLDatabasePersistActionAtomic(String title, String script) {
        this(title, script, true);
    }

    public SQLDatabasePersistActionAtomic(String title, String script, boolean makeAtomic) {
        super(title, script, true);
        this.makeAtomic = makeAtomic;
    }

    @Override
    public void beforeExecute(DBCSession session) throws DBCException {
        super.beforeExecute(session);
        if (this.makeAtomic) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
                txnManager.setAutoCommit(session.getProgressMonitor(), true);
                wasTransactional = true;
            }
        }
    }

    @Override
    public void afterExecute(DBCSession session, Throwable error) throws DBCException {
        super.afterExecute(session, error);
        if (wasTransactional) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null) {
                txnManager.setAutoCommit(session.getProgressMonitor(), false);
            }
        }
    }

}
