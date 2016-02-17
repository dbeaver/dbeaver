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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * PostgreTypeStorage
 */
public enum PostgreTypeStorage implements DBPNamedObject
{
    p("p:Plain."),
    e("e:Secondary"),
    m("m:Compressed"),
    x("x:Any");

    private final String desc;

    PostgreTypeStorage(String desc) {
        this.desc = desc;
    }

    @Override
    public String getName() {
        return desc;
    }
}
