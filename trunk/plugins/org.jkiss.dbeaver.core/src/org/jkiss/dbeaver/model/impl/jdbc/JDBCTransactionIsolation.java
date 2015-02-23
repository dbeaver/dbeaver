/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;

import java.sql.Connection;

/**
 * JDBCTransactionIsolation
 */
public enum JDBCTransactionIsolation implements DBPTransactionIsolation 
{
    NONE(Connection.TRANSACTION_NONE, CoreMessages.model_jdbc_None),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, CoreMessages.model_jdbc_read_committed),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED, CoreMessages.model_jdbc_read_uncommitted),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, CoreMessages.model_jdbc_repeatable_read),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, CoreMessages.model_jdbc_Serializable),
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
