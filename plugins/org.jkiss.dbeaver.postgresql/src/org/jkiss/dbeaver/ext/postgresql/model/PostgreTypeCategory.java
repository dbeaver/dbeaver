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
package org.jkiss.dbeaver.ext.postgresql.model;

/**
 * PostgreProcedure
 */
public enum PostgreTypeCategory 
{

    A("Array types"),
    B("Boolean types"),
    C("Composite types"),
    D("Date/time types"),
    E("Enum types"),
    G("Geometric types"),
    I("Network address types"),
    N("Numeric types"),
    P("Pseudo-types"),
    S("String types"),
    T("Timespan types"),
    U("User-defined types"),
    V("Bit-string types"),
    X("unknown type");

    private final String desc;

    PostgreTypeCategory(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
