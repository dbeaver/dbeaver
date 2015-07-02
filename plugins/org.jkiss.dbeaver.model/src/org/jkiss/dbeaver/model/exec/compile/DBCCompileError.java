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

package org.jkiss.dbeaver.model.exec.compile;

/**
 * Compile error
 */
public class DBCCompileError {

    private boolean error;
    private String message;
    private int line;
    private int position;

    public DBCCompileError(boolean error, String message, int line, int position)
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
