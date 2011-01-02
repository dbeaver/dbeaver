/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * SQLFormatterToken
 */
package org.jkiss.dbeaver.ui.editors.sql.format;

public class SQLFormatterToken {

    private int fType;
    private String fString;
    private int fPos = -1;

    public SQLFormatterToken(final int argType, final String argString, final int argPos)
    {
        setType(argType);
        setString(argString);
        setPos(argPos);
    }

    public SQLFormatterToken(final int argType, final String argString)
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
