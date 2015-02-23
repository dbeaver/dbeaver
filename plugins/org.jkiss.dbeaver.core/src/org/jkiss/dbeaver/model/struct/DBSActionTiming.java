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

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityConstraintType
 */
public class DBSActionTiming
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
        if (name.toUpperCase().equals(BEFORE.getName())) {
            return BEFORE;
        } else if (name.toUpperCase().equals(AFTER.getName())) {
            return AFTER;
        } else {
            return UNKNOWN;
        }
    }
}