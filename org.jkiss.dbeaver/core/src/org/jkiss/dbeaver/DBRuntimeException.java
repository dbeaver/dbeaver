/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver;

/**
 * DBException
 */
public class DBRuntimeException extends RuntimeException
{
    public DBRuntimeException()
    {
    }

    public DBRuntimeException(String message)
    {
        super(message);
    }

    public DBRuntimeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBRuntimeException(Throwable cause)
    {
        super(cause);
    }
}