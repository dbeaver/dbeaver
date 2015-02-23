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

/*
 * FormatterToken
 */
package org.jkiss.dbeaver.ui.editors.sql.format.tokenized;

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
