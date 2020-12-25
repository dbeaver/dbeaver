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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.messages.ModelMessages;

import java.sql.Connection;

/**
 * JDBCTransactionIsolation
 */
public enum JDBCTransactionIsolation implements DBPTransactionIsolation 
{
    NONE(Connection.TRANSACTION_NONE, ModelMessages.model_jdbc_None),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, ModelMessages.model_jdbc_read_committed),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED, ModelMessages.model_jdbc_read_uncommitted),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, ModelMessages.model_jdbc_repeatable_read),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, ModelMessages.model_jdbc_Serializable),
    ;

    private final int code;
    private final String title;

    JDBCTransactionIsolation(int code, String title)
    {
        this.code = code;
        this.title = title;
    }

    @Override
    public int getCode()
    {
        return code;
    }

    @Override
    public boolean isEnabled()
    {
        return this != NONE;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    public static JDBCTransactionIsolation getByCode(int code)
    {
        for (JDBCTransactionIsolation txni : values()) {
            if (txni.code == code) {
                return txni;
            }
        }
        return null;
    }
}
