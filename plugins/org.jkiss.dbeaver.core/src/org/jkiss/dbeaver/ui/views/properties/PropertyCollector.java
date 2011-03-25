/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * PropertyCollector
 */
public class PropertyCollector extends PropertySourceAbstract
{
    static final Log log = LogFactory.getLog(PropertyCollector.class);

    public PropertyCollector(Object sourceObject, Object object, boolean loadLazyProps)
    {
        super(sourceObject, object, loadLazyProps);
    }

    public PropertyCollector(Object object, boolean loadLazyProps)
    {
        super(object, object, loadLazyProps);
    }

    public void collectProperties()
    {
        this.collectProperties(this.getEditableValue());
    }

    public void collectProperties(Object object)
    {
        List<ObjectPropertyDescriptor> annoProps = ObjectAttributeDescriptor.extractAnnotations(object);
        for (final ObjectPropertyDescriptor desc : annoProps) {
            if (desc.isCollectionAnno()) {
                DBRRunnableWithProgress loader = new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            Object value = desc.readValue(getEditableValue(), monitor);
                            addProperty(desc.getId(), desc.getDisplayName(), value);
                        } catch (IllegalAccessException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                // Add as collection
                try {
                    if (desc.isLazy(true)) {
                        DBeaverCore.getInstance().runInProgressService(loader);
                    } else {
                        loader.run(null);
                    }
                }
                catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // Do nothing
                }
            } else {
                addProperty(desc);
            }
        }
    }

}
