/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
    private final String name;

    JDBCTransactionIsolation(int code, String name)
    {
        this.code = code;
        this.name = name;
    }

    public int getCode()
    {
        return code;
    }

    public boolean isEnabled()
    {
        return this != NONE;
    }

    public String getName()
    {
        return name;
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
