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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

/**
 * ProxyPropertyDescriptor
*/
public class ProxyPropertyDescriptor implements DBPPropertyDescriptor
{

    protected final DBPPropertyDescriptor original;

    public ProxyPropertyDescriptor(DBPPropertyDescriptor original)
    {
        this.original = original;
    }

    @NotNull
    @Override
    public Object getId()
    {
        return this.original.getId();
    }

    @Override
    public String getCategory()
    {
        return this.original.getCategory();
    }

    @Override
    public String getDescription()
    {
        return this.original.getDescription();
    }

    @Override
    public Class<?> getDataType() {
        return original.getDataType();
    }

    @Override
    public boolean isRequired() {
        return original.isRequired();
    }

    @Override
    public boolean isRemote() {
        return original.isRemote();
    }

    @Override
    public Object getDefaultValue() {
        return original.getDefaultValue();
    }

    @Override
    public boolean isEditable(Object object) {
        return original.isEditable(object);
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        return this.original.getDisplayName();
    }


}
