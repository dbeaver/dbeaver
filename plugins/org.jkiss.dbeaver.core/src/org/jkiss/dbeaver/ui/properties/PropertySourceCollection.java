/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.Collection;

/**
 *
 */
class PropertySourceCollection extends PropertySourceAbstract {

    private PropertySourceAbstract parentSource;
    private Object id;

    PropertySourceCollection(PropertySourceAbstract parentSource, Object id, boolean loadLazyProps, Collection<?> value)
    {
        super(value, value, loadLazyProps);
        this.parentSource = parentSource;
        this.id = id;

        int propIndex = 0;
        for (Object item : value) {
            String itemId = id + "-" + propIndex;
            String itemName;
            if (item instanceof DBPNamedObject) {
                itemName = ((DBPNamedObject)item).getName();
            } else {
                itemName = item == null ? "[NULL]" : item.toString();
            }
            addProperty(itemId, itemName, item);
            propIndex++;
        }
    }

    public Object getId()
    {
        return id;
    }

    @Override
    public Object getEditableValue()
    {
        return "[" + ((Collection<?>)super.getEditableValue()).size() + "]"; 
    }

}
