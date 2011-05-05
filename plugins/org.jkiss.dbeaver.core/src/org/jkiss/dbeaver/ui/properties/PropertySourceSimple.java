/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceSimple implements IPropertySourceEx {

    private List<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>();

    private Map<Object, Object> originalValues = new TreeMap<Object, Object>();
    private Map<Object, Object> propValues = new TreeMap<Object, Object>();
    private Map<Object,Object> defaultValues = new TreeMap<Object, Object>();

    public void setValues(Map<Object, Object> originalValues)
    {
        this.originalValues = originalValues;
    }

    public void setDefaultValues(Map<Object, Object> defaultValues)
    {
        this.defaultValues = defaultValues;
    }

    public IPropertyDescriptor addProperty(Object id, String displayName, String description, String category)
    {
        return addProperty(id, displayName, description, category, null, null);
    }

    public IPropertyDescriptor addProperty(Object id, String displayName, String description, String category, String[] filterFlags, Object helpContextIds)
    {
        PropertyDescriptor prop = new PropertyDescriptor(id, displayName, description, category, filterFlags, helpContextIds);
        props.add(prop);
        return prop;
    }

    public Object getEditableValue()
    {
        return this;
    }

    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    public Object getPropertyValue(Object id)
    {
        Object value = propValues.get(id);
        if (value == null) {
            value = originalValues.get(id);
        }
        return value != null ? value : defaultValues.get(id);
    }

    public boolean isPropertyResettable(Object id)
    {
        return true;
    }

    public boolean isPropertySet(Object id)
    {
        return propValues.containsKey(id) || originalValues.containsKey(id);
    }

    public void resetPropertyValue(Object id)
    {
        propValues.remove(id);
    }

    public void setPropertyValue(Object id, Object value)
    {
        propValues.put(id, value);
    }

    public boolean hasDefaultValue(Object id)
    {
        return defaultValues.containsKey(id);
    }

    public void resetPropertyValueToDefault(Object id)
    {
        propValues.remove(id);
        originalValues.remove(id);
    }

    private static class PropertyDescriptor implements IPropertyDescriptor {

        private String category;
        private String description;
        private String displayName;
        private String[] filterFlags;
        private Object helpContextIds;
        private Object id;

        private PropertyDescriptor(Object id, String displayName, String description, String category, String[] filterFlags, Object helpContextIds)
        {
            this.category = category;
            this.description = description;
            this.displayName = displayName;
            this.filterFlags = filterFlags;
            this.helpContextIds = helpContextIds;
            this.id = id;
        }

        public CellEditor createPropertyEditor(Composite parent)
        {
            return null;
        }

        public String getCategory()
        {
            return category;
        }

        public String getDescription()
        {
            return description;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public String[] getFilterFlags()
        {
            return filterFlags;
        }

        public Object getHelpContextIds()
        {
            return helpContextIds;
        }

        public Object getId()
        {
            return id;
        }

        public ILabelProvider getLabelProvider()
        {
            return null;
        }

        public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
        {
            return anotherProperty instanceof PropertyDescriptor && anotherProperty.getId().equals(id);
        }
    }
    
}
