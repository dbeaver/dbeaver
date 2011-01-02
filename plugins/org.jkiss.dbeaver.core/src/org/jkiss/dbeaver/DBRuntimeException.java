/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver;

/**
 * DBException
 */
public class DBRuntimeException extends RuntimeException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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