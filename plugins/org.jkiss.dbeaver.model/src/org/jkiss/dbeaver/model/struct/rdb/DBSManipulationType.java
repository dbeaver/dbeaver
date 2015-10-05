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

package org.jkiss.dbeaver.model.struct.rdb;

import java.util.Locale;

/**
 * DBSManipulationType
 */
public class DBSManipulationType
{
    public static final DBSManipulationType INSERT = new DBSManipulationType("INSERT");
    public static final DBSManipulationType DELETE = new DBSManipulationType("DELETE");
    public static final DBSManipulationType UPDATE = new DBSManipulationType("UPDATE");
    public static final DBSManipulationType UNKNOWN = new DBSManipulationType("UNKNOWN");

    private final String name;

    protected DBSManipulationType(String name)
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

    public static DBSManipulationType getByName(String name)
    {
        if (name.toUpperCase(Locale.ENGLISH).equals(INSERT.getName())) {
            return INSERT;
        } else if (name.toUpperCase(Locale.ENGLISH).equals(DELETE.getName())) {
            return DELETE;
        } if (name.toUpperCase(Locale.ENGLISH).equals(UPDATE.getName())) {
            return UPDATE;
        } else {
            return UNKNOWN;
        }
    }
}