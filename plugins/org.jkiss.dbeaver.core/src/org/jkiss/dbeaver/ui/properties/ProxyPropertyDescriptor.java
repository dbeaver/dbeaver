/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
