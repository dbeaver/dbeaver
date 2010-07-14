/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPObject;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * PropertyCollector
 */
public class PropertyCollector extends PropertySourceAbstract
{
    static final Log log = LogFactory.getLog(PropertyCollector.class);

    public PropertyCollector(DBPObject object, boolean loadLazyProps)
    {
        super(object, loadLazyProps);
    }

    public void collectProperties()
    {
        this.collectProperties(this.getEditableValue());
    }

    public void collectProperties(Object object)
    {
        List<PropertyAnnoDescriptor> annoProps = PropertyAnnoDescriptor.extractAnnotations(object);
        for (PropertyAnnoDescriptor desc : annoProps) {
            if (desc.isCollectionAnno()) {
                // Add as collection
                try {
                    Object value = desc.readValue(getEditableValue(), null);
                    addProperty(desc.getId(), desc.getDisplayName(), value);
                }
                catch (IllegalAccessException e) {
                    log.error(e);
                }
                catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                }
            } else {
                addProperty(desc);
            }
        }
    }

}
