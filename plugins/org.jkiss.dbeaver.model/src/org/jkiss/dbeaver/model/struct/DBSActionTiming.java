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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.Locale;

/**
 * DBSEntityConstraintType
 */
public class DBSActionTiming implements DBPNamedObject
{
    public static final DBSActionTiming BEFORE = new DBSActionTiming("BEFORE");
    public static final DBSActionTiming AFTER = new DBSActionTiming("AFTER");
    public static final DBSActionTiming UNKNOWN = new DBSActionTiming("UNKNOWN");

    private final String name;

    protected DBSActionTiming(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString()
    {
        return getName();
    }

    public static DBSActionTiming getByName(String name)
    {
        if (name.toUpperCase(Locale.ENGLISH).equals(BEFORE.getName())) {
            return BEFORE;
        } else if (name.toUpperCase(Locale.ENGLISH).equals(AFTER.getName())) {
            return AFTER;
        } else {
            return UNKNOWN;
        }
    }
}