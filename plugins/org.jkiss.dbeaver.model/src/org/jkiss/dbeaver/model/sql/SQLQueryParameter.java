/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql;

/**
 * SQL statement parameter info
 */
public class SQLQueryParameter {
    private int ordinalPosition;
    private String name;
    private String value;
    private int tokenOffset;
    private int tokenLength;
    private SQLQueryParameter previous;

    public SQLQueryParameter(int ordinalPosition, String name, int tokenOffset, int tokenLength)
    {
        this.ordinalPosition = ordinalPosition;
        this.name = name.trim();
        this.tokenOffset = tokenOffset;
        this.tokenLength = tokenLength;
    }

    public boolean isNamed() {
        return !"?".equals(name);
    }

    public int getTokenOffset() {
        return tokenOffset;
    }

    public int getTokenLength() {
        return tokenLength;
    }

    public SQLQueryParameter getPrevious() {
        return previous;
    }

    public void setPrevious(SQLQueryParameter previous) {
        this.previous = previous;
    }

    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getTitle() {
        if (name.startsWith(":")) {
            return name.substring(1);
        } else {
            return name;
        }
    }

    @Override
    public String toString() {
        return getTitle() + "=" + value;
    }
}
