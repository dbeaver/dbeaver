/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.jkiss.dbeaver.model.DBPObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.lang.reflect.InvocationTargetException;

/**
 * PropertyCollector
 */
public class PropertyCollector extends PropertySourceAbstract
{
    static Log log = LogFactory.getLog(PropertyCollector.class);

    public PropertyCollector(DBPObject object)
    {
        this(object, true);
    }

    public PropertyCollector(DBPObject object, boolean loadLazyObjects)
    {
        super(object, loadLazyObjects);
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
                    Object value = desc.readValue(getEditableValue());
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
