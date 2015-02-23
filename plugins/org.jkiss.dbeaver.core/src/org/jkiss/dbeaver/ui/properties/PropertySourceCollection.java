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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCollection implements IPropertySource {

    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();

    private List<Object> items;

    public PropertySourceCollection(Collection<?> collection)
    {
        items = new ArrayList<Object>(collection);
        for (int i = 0; i < items.size(); i++) {
            //props.addAll(ObjectPropertyDescriptor.extractAnnotations(this, item.getClass(), null));
            props.add(new ItemPropertyDescriptor(i, items.get(i)));
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    @Override
    public Object getPropertyValue(Object id)
    {
        return items.get((Integer) id);
    }

    @Override
    public boolean isPropertySet(Object id)
    {
        return false;
    }

    @Override
    public void resetPropertyValue(Object id)
    {

    }

    @Override
    public void setPropertyValue(Object id, Object value)
    {
    }

    private class ItemPropertyDescriptor implements IPropertyDescriptor {
        private Integer id;
        private Object item;

        private ItemPropertyDescriptor(Integer id, Object item) {
            this.id = id;
            this.item = item;
        }

        @Override
        public CellEditor createPropertyEditor(Composite composite) {
            return null;
        }

        @Override
        public String getCategory() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return DBUtils.getObjectShortName(item);
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
            return id;
        }

        @Override
        public ILabelProvider getLabelProvider() {
            return null;
        }

        @Override
        public boolean isCompatibleWith(IPropertyDescriptor prop) {
            return this.equals(prop);
        }
    }
}
