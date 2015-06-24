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
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * PropertyDescriptorDelegate
 */
public class PropertyDescriptorDelegate implements IPropertyDescriptor
{
    private final DBPPropertySource propSource;
    private final DBPPropertyDescriptor source;

    public PropertyDescriptorDelegate(DBPPropertySource propSource, DBPPropertyDescriptor source) {
        this.propSource = propSource;
        this.source = source;
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return UIUtils.createCellEditor(parent, propSource.getEditableValue(), source);
    }

    @Override
    public String getCategory() {
        return source.getCategory();
    }

    @Override
    public String getDescription() {
        return source.getDescription();
    }

    @Override
    public String getDisplayName() {
        return source.getDisplayName();
    }

    @Override
    public String[] getFilterFlags() {
        return null;
    }

    @Override
    public Object getHelpContextIds() {
        return null;
    }

    @Override
    public Object getId() {
        return source.getId();
    }

    @Override
    public ILabelProvider getLabelProvider() {
        return null;
    }

    @Override
    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
        return false;
    }
}