/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PropertyCollector
 */
public abstract class PropertySourceAbstract implements IPropertySource
{
    private Object sourceObject;
    private Object object;
    private boolean loadLazyProps;
    private final List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private final Map<Object, Object> propValues = new HashMap<Object, Object>();
    private final List<ObjectPropertyDescriptor> lazyProps = new ArrayList<ObjectPropertyDescriptor>();
    private LoadingJob<Map<ObjectPropertyDescriptor, Object>> lazyLoadJob;

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
            propValues.put(id, new PropertySourceCollection(this, id, loadLazyProps, (Collection<?>) value));
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
                if (annoDescriptor.isLazy(object, true)) {
                    if (!loadLazyProps) {
                        return null;
                    } else {
                        synchronized (lazyProps) {
                            lazyProps.add(annoDescriptor);
                            if (lazyLoadJob == null) {
                                // We assume that it can be called ONLY by properties viewer
                                // So, start lazy loading job to update it after value will be loaded
                                lazyLoadJob = LoadingUtils.createService(
                                    new PropertySheetLoadService(),
                                    new PropertySheetLoadVisualizer());
                                lazyLoadJob.addJobChangeListener(new JobChangeAdapter() {
                                    @Override
                                    public void done(IJobChangeEvent event)
                                    {
                                        synchronized (lazyProps) {
                                            if (!lazyProps.isEmpty()) {
                                                lazyLoadJob.schedule(100);
                                            } else {
                                                lazyLoadJob = null;
                                            }
                                        }
                                    }
                                });
                                lazyLoadJob.schedule(100);
                            }
                        }
                        // Return dummy string for now
                        propValues.put(id, PropertySheetLoadService.TEXT_LOADING);
                        return PropertySheetLoadService.TEXT_LOADING;
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
        if (getEditableValue() instanceof DBPObject) {
            DBEObjectCommander objectCommander = getObjectCommander();
            if (objectCommander != null) {
                objectCommander.addCommand(
                    new DBECommandProperty<DBPObject>(
                        (DBPObject) getEditableValue(),
                        null,
                        value),
                    null);
            }
        }
    }

    protected abstract DBEObjectCommander getObjectCommander();

    protected abstract DBEObjectManager getObjectManager();

    private class PropertySheetLoadService extends AbstractLoadService<Map<ObjectPropertyDescriptor, Object>> {

        public static final String TEXT_LOADING = "...";

        public PropertySheetLoadService()
        {
            super(TEXT_LOADING);
        }

        public Map<ObjectPropertyDescriptor, Object> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                Map<ObjectPropertyDescriptor, Object> result = new IdentityHashMap<ObjectPropertyDescriptor, Object>();
                for (ObjectPropertyDescriptor prop : obtainLazyProperties()) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
                    result.put(prop, prop.readValue(object, getProgressMonitor()));
                }
                return result;
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

    private List<ObjectPropertyDescriptor> obtainLazyProperties()
    {
        synchronized (lazyProps) {
            if (lazyProps.isEmpty()) {
                return Collections.emptyList();
            } else {
                List<ObjectPropertyDescriptor> result = new ArrayList<ObjectPropertyDescriptor>(lazyProps);
                lazyProps.clear();
                return result;
            }
        }
    }

    private class PropertySheetLoadVisualizer implements ILoadVisualizer<Map<ObjectPropertyDescriptor, Object>> {
        //private Object propertyId;
        private int callCount = 0;
        private boolean completed = false;

        private PropertySheetLoadVisualizer()
        {
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
/*
            String dots;
            switch (callCount++ % 4) {
            case 0: dots = ""; break;
            case 1: dots = "."; break;
            case 2: dots = ".."; break;
            case 3: default: dots = "..."; break;
            }
            propValues.put(propertyId, PropertySheetLoadService.TEXT_LOADING + dots);
            refreshProperties(false);
*/
        }

        public void completeLoading(Map<ObjectPropertyDescriptor, Object> result)
        {
            completed = true;
            for (Map.Entry<ObjectPropertyDescriptor, Object> entry : result.entrySet()) {
                propValues.put(entry.getKey().getId(), entry.getValue());
                PropertiesContributor.getInstance().notifyPropertyLoad(sourceObject, entry.getKey().getId(), entry.getValue(), true);
            }
        }

    }
}