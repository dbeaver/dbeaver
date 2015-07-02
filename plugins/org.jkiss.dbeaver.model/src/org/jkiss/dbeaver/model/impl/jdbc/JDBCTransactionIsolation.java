/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;

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
