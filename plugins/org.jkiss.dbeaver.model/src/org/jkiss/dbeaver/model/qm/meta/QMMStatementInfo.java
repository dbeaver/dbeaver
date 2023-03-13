/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

    private final QMMConnectionInfo connection;
    private final DBCExecutionPurpose purpose;
    private final transient QMMStatementInfo previous;

    private transient DBCStatement reference;

    public QMMStatementInfo(QMMConnectionInfo connection, DBCStatement reference, QMMStatementInfo previous) {
        super(QMMetaObjectType.STATEMENT_INFO);
        this.connection = connection;
        this.reference = reference;
        this.purpose = reference.getSession().getPurpose();
        this.previous = previous;
    }

    public QMMStatementInfo(long openTime, long closeTime, QMMConnectionInfo session, DBCExecutionPurpose purpose) {
        super(QMMetaObjectType.STATEMENT_INFO, openTime, closeTime);
        this.connection = session;
        this.purpose = purpose;
        this.previous = null;
    }

    private QMMStatementInfo(Builder builder) {
        super(QMMetaObjectType.STATEMENT_INFO, builder.openTime, builder.closeTime);
        connection = builder.connection;
        purpose = builder.purpose;
        previous = builder.previous;
        reference = builder.reference;
    }

    @Override
    public void close()
    {
        super.close();
        reference = null;
    }

    @Override
    public String getText() {
        return connection.getText();
    }

    @Override
    public QMMetaObjectType getObjectType() {
        return QMMetaObjectType.STATEMENT_INFO;
    }

    DBCStatement getReference() {
        return reference;
    }

    public QMMConnectionInfo getConnection() {
        return connection;
    }

    public DBCExecutionPurpose getPurpose() {
        return purpose;
    }

    public QMMStatementInfo getPrevious() {
        return previous;
    }

    @Override
    public String toString()
    {
        return "STATEMENT";
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final class Builder {
        private QMMConnectionInfo connection;
        private DBCExecutionPurpose purpose;
        private QMMStatementInfo previous;
        private DBCStatement reference;
        private long openTime;
        private long closeTime;

        public Builder() {
        }

        public Builder(QMMStatementInfo copy) {
            this.connection = copy.getConnection();
            this.purpose = copy.getPurpose();
            this.previous = copy.getPrevious();
            this.reference = copy.getReference();
        }

        public Builder setConnection(QMMConnectionInfo connection) {
            this.connection = connection;
            return this;
        }

        public Builder setPurpose(DBCExecutionPurpose purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder setPrevious(QMMStatementInfo previous) {
            this.previous = previous;
            return this;
        }

        public Builder setReference(DBCStatement reference) {
            this.reference = reference;
            return this;
        }


        public Builder setOpenTime(long openTime) {
            this.openTime = openTime;
            return this;
        }

        public Builder setCloseTime(long closeTime) {
            this.closeTime = closeTime;
            return this;
        }

        public QMMStatementInfo build() {
            return new QMMStatementInfo(this);
        }
    }
}
