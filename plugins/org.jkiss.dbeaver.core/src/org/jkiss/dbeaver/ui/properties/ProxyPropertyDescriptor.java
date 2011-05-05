/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public Object getId()
    {
        return this.original.getId();
    }

    public CellEditor createPropertyEditor(Composite parent)
    {
        return this.original.createPropertyEditor(parent);
    }

    public String getCategory()
    {
        return this.original.getCategory();
    }

    public String getDescription()
    {
        return this.original.getDescription();
    }

    public String getDisplayName()
    {
        return this.original.getDisplayName();
    }

    public String[] getFilterFlags()
    {
        return this.original.getFilterFlags();
    }

    public Object getHelpContextIds()
    {
        return this.original.getHelpContextIds();
    }

    public ILabelProvider getLabelProvider()
    {
        return this.original.getLabelProvider();
    }

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return this.original.isCompatibleWith(anotherProperty);
    }

}
