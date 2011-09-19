/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Compile error
 */
public class OracleCompileError {

    private boolean error;
    private String message;
    private int line;
    private int position;

    public OracleCompileError(boolean error, String message, int line, int position)
    {
        this.error = error;
        this.message = message;
        this.line = line;
        this.position = position;
    }

    public boolean isError()
    {
        return error;
    }

    public String getMessage()
    {
        return message;
    }

    public int getLine()
    {
        return line;
    }

    public int getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        if (line <= 0) {
            return message;
        } else {
            return "[" + line + ":" + position + "] " + message;
        }
    }
}
