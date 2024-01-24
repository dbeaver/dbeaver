/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.qm.QMConstants;

public enum QMMetaObjectType {
    CONNECTION_INFO(QMConstants.EVENT_TYPE_SESSION, QMMConnectionInfo.class),
    TRANSACTION_INFO(QMConstants.EVENT_TYPE_TXN, QMMTransactionInfo.class),
    TRANSACTION_SAVEPOINT_INFO(QMConstants.EVENT_TYPE_SAVEPOINT, QMMTransactionSavepointInfo.class),
    STATEMENT_INFO(QMConstants.EVENT_TYPE_STATEMENT, QMMStatementInfo.class),
    STATEMENT_EXECUTE_INFO(QMConstants.EVENT_TYPE_EXECUTE, QMMStatementExecuteInfo.class);

    private final int id;
    private final Class<? extends QMMObject> objectClass;

    QMMetaObjectType(int id, Class<? extends QMMObject> objectClass) {
        this.id = id;
        this.objectClass = objectClass;
    }

    public int getId() {
        return id;
    }

    public Class<? extends QMMObject> getObjectClass() {
        return objectClass;
    }
}
