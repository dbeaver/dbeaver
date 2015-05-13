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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

/**
 * ProxyPropertyDescriptor
*/
public class ProxyPropertyDescriptor implements IPropertyDescriptor
{

    protected final IPropertyDescriptor original;

    public ProxyPropertyDescriptor(IPropertyDescriptor original)
    {
        this.original = original;
    }

    @Override
    public Object getId()
    {
        return this.original.getId();
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent)
    {
        return this.original.createPropertyEditor(parent);
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
    public String getDisplayName()
    {
        return this.original.getDisplayName();
    }

    @Override
    public String[] getFilterFlags()
    {
        return this.original.getFilterFlags();
    }

    @Override
    public Object getHelpContextIds()
    {
        return this.original.getHelpContextIds();
    }

    @Override
    public ILabelProvider getLabelProvider()
    {
        return this.original.getLabelProvider();
    }

    @Override
    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return this.original.isCompatibleWith(anotherProperty);
    }

}
