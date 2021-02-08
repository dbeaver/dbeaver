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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatement;

/**
 * DBCStatement meta info
 */
public class QMMStatementInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final DBCExecutionPurpose purpose;
    private final QMMStatementInfo previous;

    private transient DBCStatement reference;

    public QMMStatementInfo(QMMSessionInfo session, DBCStatement reference, QMMStatementInfo previous)
    {
        this.session = session;
        this.reference = reference;
        this.purpose = reference.getSession().getPurpose();
        this.previous = previous;
    }

    public QMMStatementInfo(long openTime, long closeTime, QMMSessionInfo session, DBCExecutionPurpose purpose) {
        super(openTime, closeTime);
        this.session = session;
        this.purpose = purpose;
        this.previous = null;
    }

    @Override
    public void close()
    {
        super.close();
        reference = null;
    }

    @Override
    public String getText() {
        return session.getText();
    }

    DBCStatement getReference()
    {
        return reference;
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    public QMMStatementInfo getPrevious()
    {
        return previous;
    }

    @Override
    public String toString()
    {
        return "STATEMENT";
    }

}
