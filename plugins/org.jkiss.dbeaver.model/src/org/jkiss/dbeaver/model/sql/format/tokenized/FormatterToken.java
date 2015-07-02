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

/*
 * FormatterToken
 */
package org.jkiss.dbeaver.model.sql.format.tokenized;

class FormatterToken {

    private int fType;
    private String fString;
    private int fPos = -1;

    public FormatterToken(final int argType, final String argString, final int argPos)
    {
        setType(argType);
        setString(argString);
        setPos(argPos);
    }

    public FormatterToken(final int argType, final String argString)
    {
        this(argType, argString, -1);
    }

    public void setType(final int argType)
    {
        fType = argType;
    }

    public int getType()
    {
        return fType;
    }

    public void setString(final String argString)
    {
        fString = argString;
    }

    public String getString()
    {
        return fString;
    }

    public void setPos(final int argPos)
    {
        fPos = argPos;
    }

    public int getPos()
    {
        return fPos;
    }

    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(getClass().getName());
        buf.append("type=").append(fType);
        buf.append(",string=").append(fString);
        buf.append(",pos=").append(fPos);
        buf.append("]");
        return buf.toString();
    }
}
