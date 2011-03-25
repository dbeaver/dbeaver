/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PropertyCollector
 */
public class PropertySourceAbstract implements IPropertySource
{
    private Object sourceObject;
    private Object object;
    private boolean loadLazyProps;
    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private Map<Object, Object> propValues = new HashMap<Object, Object>();

    /**
     * constructs property source
     * @param object object
     */
    public PropertySourceAbstract(Object sourceObject, Object object, boolean loadLazyProps)
    {
        this.sourceObject = sourceObject;
        this.object = object;
        this.loadLazyProps = loadLazyProps;
    }

    public PropertySourceAbstract addProperty(IPropertyDescriptor prop)
    {
        props.add(prop);
        propValues.put(prop.getId(), prop);
        return this;
    }

    public PropertySourceAbstract addProperty(Object id, String name, Object value)
    {
        if (value instanceof Collection) {
            props.add(new PropertyDescriptor(id, name));
            propValues.put(id, new PropertySourceCollection(id, loadLazyProps, (Collection<?>) value));
        } else {
            props.add(new PropertyDescriptor(id, name));
            propValues.put(id, value);
        }
        return this;
    }

    public boolean isEmpty()
    {
        return props.isEmpty();
    }

    public Object getEditableValue()
    {
        return object;
    }

    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    public Object getPropertyValue(final Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            try {
                ObjectPropertyDescriptor annoDescriptor = (ObjectPropertyDescriptor) value;
                if (annoDescriptor.isLazy(true)) {
                    if (!loadLazyProps) {
                        return null;
                    } else {
                        // We assume that it can be called ONLY by properties viewer
                        // So, start lazy loading job to update it after value will be loaded
                        String loadText = "Loading";
                        LoadingUtils.createService(
                            new PropertySheetLoadService(annoDescriptor),
                            new PropertySheetLoadVisualizer(id, loadText))
                            .schedule();
                        // Return dummy string for now
                        propValues.put(id, loadText);
                        return loadText;
                    }
                } else {
                    value = annoDescriptor.readValue(object, null);
                }
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (value instanceof Collection) {
            // Make descriptor of collection
            // Each element as separate property
            Collection<?> collection = (Collection<?>)value;
            collection.size();
        }
        return UIUtils.makeStringForUI(value);
    }

    public boolean isPropertySet(Object id)
    {
        return false;
    }

    public void resetPropertyValue(Object id)
    {
    }

    public void setPropertyValue(Object id, Object value)
    {
    }

    private class PropertySheetLoadService extends AbstractLoadService<Object> {

        private ObjectPropertyDescriptor annoDescriptor;
        public PropertySheetLoadService(ObjectPropertyDescriptor annoDescriptor)
        {
            super("Loading");
            this.annoDescriptor = annoDescriptor;
        }

        public Object evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                return annoDescriptor.readValue(object, getProgressMonitor());
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }

        public Object getFamily() {
            return object instanceof DBSObject ? ((DBSObject)object).getDataSource() : object;
        }
    }


    private class PropertySheetLoadVisualizer implements ILoadVisualizer<Object> {
        private Object propertyId;
        private String loadText;
        private int callCount = 0;
        private boolean completed = false;

        private PropertySheetLoadVisualizer(Object propertyId, String loadText)
        {
            this.propertyId = propertyId;
            this.loadText = loadText;
        }

        public Shell getShell() {
            return UIUtils.getActiveShell();
        }

        public DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor)
        {
            return monitor;
        }

        public boolean isCompleted()
        {
            return completed;
        }

        public void visualizeLoading()
        {
            String dots;
            switch (callCount++ % 4) {
            case 0: dots = ""; break;
            case 1: dots = "."; break;
            case 2: dots = ".."; break;
            case 3: default: dots = "..."; break;
            }
            propValues.put(propertyId, loadText + dots);
            refreshProperties(false);
        }

        public void completeLoading(Object result)
        {
            completed = true;
            propValues.put(propertyId, result);
            refreshProperties(true);
        }

        private void refreshProperties(boolean completed)
        {
            PropertiesContributor.getInstance().notifyPropertyLoad(sourceObject, propertyId, propValues.get(propertyId), completed);
        }

    }
}