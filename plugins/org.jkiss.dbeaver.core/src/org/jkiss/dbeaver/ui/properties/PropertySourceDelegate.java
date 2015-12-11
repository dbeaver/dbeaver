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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPPropertySource;

/**
 * PropertySourceDelegate
 */
public class PropertySourceDelegate implements IPropertySource2
{
    private final DBPPropertySource source;

    public PropertySourceDelegate(DBPPropertySource source) {
        this.source = source;
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return source.isPropertyResettable(id);
    }

    @Override
    public Object getEditableValue() {
        return source.getEditableValue();
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        DBPPropertyDescriptor[] src = source.getPropertyDescriptors2();
        if (src == null) {
            return null;
        }
        IPropertyDescriptor[] dst = new IPropertyDescriptor[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = new PropertyDescriptorDelegate(source, src[i]);
        }
        return dst;
    }

    @Override
    public Object getPropertyValue(Object id) {
        return source.getPropertyValue(null, id);
    }

    @Override
    public boolean isPropertySet(Object id) {
        return source.isPropertySet(id);
    }

    @Override
    public void resetPropertyValue(Object id) {
        source.resetPropertyValue(null, id);
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        source.setPropertyValue(null, id, value);
    }
}