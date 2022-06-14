/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMConnectionInfo connection;
    private final QMMTransactionInfo previous;
    private boolean committed;
    private QMMTransactionSavepointInfo savepointStack;

    QMMTransactionInfo(QMMConnectionInfo connection, QMMTransactionInfo previous) {
        this.connection = connection;
        this.previous = previous;
        this.savepointStack = new QMMTransactionSavepointInfo(this, null, null, null);
    }

    private QMMTransactionInfo(Builder builder) {
        connection = builder.connection;
        previous = builder.previous;
        committed = builder.committed;
        savepointStack = builder.savepointStack;
    }

    public static Builder builder() {
        return new Builder();
    }

    void commit() {
        this.committed = true;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            if (!sp.isClosed()) {
                // Commit all non-finished savepoints
                sp.close(true);
            }
        }
        super.close();
    }

    void rollback(DBCSavepoint toSavepoint)
    {
        this.committed = false;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            sp.close(false);
            if (toSavepoint != null && sp.getReference() == toSavepoint) {
                break;
            }
        }
        super.close();
    }

    public QMMConnectionInfo getConnection() {
        return connection;
    }

    @Override
    public Map<String, Object> toMap() throws DBException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("connection", getConnection().toMap());
        return result;
    }

    public static QMMTransactionInfo fromMap(Map<String, Object> objectMap) {
        QMMConnectionInfo connectionInfo = QMMConnectionInfo.fromMap((Map<String, Object>) objectMap.get("connection"));
        return builder().setConnection(connectionInfo).build();
    }

    public QMMTransactionInfo getPrevious() {
        return previous;
    }

    public boolean isCommitted() {
        return committed;
    }

    public QMMTransactionSavepointInfo getCurrentSavepoint()
    {
        return savepointStack;
    }

    public QMMObject getSavepoint(DBCSavepoint savepoint)
    {
        for (QMMTransactionSavepointInfo sp = this.savepointStack; sp != null; sp = sp.getPrevious()) {
            if (sp.getReference() == savepoint) {
                return sp;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "TRANSACTION";
    }

    @Override
    public String getText() {
        return connection.getText();
    }

    public static final class Builder {
        private QMMConnectionInfo connection;
        private QMMTransactionInfo previous;
        private boolean committed;
        private QMMTransactionSavepointInfo savepointStack;

        private Builder() {
        }

        public Builder setConnection(QMMConnectionInfo connection) {
            this.connection = connection;
            return this;
        }

        public Builder setPrevious(QMMTransactionInfo previous) {
            this.previous = previous;
            return this;
        }

        public Builder setCommitted(boolean committed) {
            this.committed = committed;
            return this;
        }

        public Builder setSavepointStack(QMMTransactionSavepointInfo savepointStack) {
            this.savepointStack = savepointStack;
            return this;
        }

        public QMMTransactionInfo build() {
            return new QMMTransactionInfo(this);
        }
    }
}
