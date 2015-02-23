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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.properties.PropertiesContributor;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PropertyCollector
 */
public abstract class PropertySourceAbstract implements IPropertySourceMulti
{
    static final Log log = Log.getLog(PropertySourceAbstract.class);

    private Object sourceObject;
    private Object object;
    private boolean loadLazyProps;
    private final List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private final Map<Object, Object> propValues = new HashMap<Object, Object>();
    private final Map<Object, Object> lazyValues = new HashMap<Object, Object>();
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
        if (prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isHidden()) {
            // Do not add it to property list
        } else {
            props.add(prop);
        }
        propValues.put(prop.getId(), prop);
        return this;
    }

    public PropertySourceAbstract addProperty(@Nullable String category, Object id, String name, Object value)
    {
        props.add(new PropertyDescriptorEx(category, id, name, null, value == null ? null : value.getClass(), false, null, null, false));
        propValues.put(id, value);

        return this;
    }

    public void clearProperties()
    {
        props.clear();
        propValues.clear();
    }

    public boolean hasProperty(ObjectPropertyDescriptor prop)
    {
        return props.contains(prop);
    }

    public boolean isEmpty()
    {
        return props.isEmpty();
    }

    public Object getSourceObject()
    {
        return sourceObject;
    }

    @Override
    public Object getEditableValue()
    {
        return object;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

/*
    public IPropertyDescriptor getPropertyDescriptor(final Object id)
    {
        for (IPropertyDescriptor prop : props) {
            if (CommonUtils.equalObjects(prop.getId(), id)) {
                return prop;
            }
        }
        return null;
    }
*/

    @Override
    public boolean isPropertySet(Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            return isPropertySet(getEditableValue(), (ObjectPropertyDescriptor) value);
        } else {
            return value != null;
        }
    }

    @Override
    public boolean isPropertySet(Object object, ObjectPropertyDescriptor prop)
    {
        try {
            return !prop.isLazy(object, true) && prop.readValue(object, null) != null;
        } catch (Exception e) {
            log.error("Error reading property '" + prop.getId() + "' from " + object, e);
            return false;
        }
    }

    @Override
    public final Object getPropertyValue(final Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            value = getPropertyValue(getEditableValue(), (ObjectPropertyDescriptor) value);
        }
        return value;
    }


    @Override
    public Object getPropertyValue(final Object object, final ObjectPropertyDescriptor prop)
    {
        try {
            if (prop.isLazy(object, true) && !prop.supportsPreview()) {
                final Object value = lazyValues.get(prop.getId());
                if (value != null) {
                    return value;
                }
                if (lazyValues.containsKey(prop.getId())) {
                    // Some lazy props has null value
                    return null;
                }
                if (!loadLazyProps) {
                    return null;
                } else {
                    synchronized (lazyProps) {
                        lazyProps.add(prop);
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
                    lazyValues.put(prop.getId(), null);
                    return null;
                }
            } else {
                return prop.readValue(object, null);
            }
        } catch (Exception e) {
            log.error("Error reading property '" + prop.getId() + "' from " + object, e);
            return e.getMessage();
        }
    }

    @Override
    public boolean isPropertyResettable(Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            return isPropertyResettable(getEditableValue(), (ObjectPropertyDescriptor) value);
        } else {
            // No by default
            return false;
        }
    }

    @Override
    public boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop)
    {
        return false;
    }

    @Override
    public final void resetPropertyValue(Object id)
    {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            resetPropertyValue(getEditableValue(), (ObjectPropertyDescriptor) value);
        } else {
            throw new UnsupportedOperationException("Direct property reset not implemented");
        }
    }

    @Override
    public void resetPropertyValue(Object object, ObjectPropertyDescriptor id)
    {
        throw new UnsupportedOperationException("Cannot reset property in non-editable property source");
    }

    @Override
    public final void setPropertyValue(Object id, Object value)
    {
        Object prop = propValues.get(id);
        if (prop instanceof ObjectPropertyDescriptor) {
            setPropertyValue(getEditableValue(), (ObjectPropertyDescriptor) prop, value);
            lazyValues.put(((ObjectPropertyDescriptor) prop).getId(), value);
        } else {
            propValues.put(id, value);
        }
    }

    @Override
    public void setPropertyValue(Object object, ObjectPropertyDescriptor prop, Object value)
    {
        throw new UnsupportedOperationException("Cannot update property in non-editable property source");
    }

    public boolean collectProperties()
    {
        lazyValues.clear();
        props.clear();
        propValues.clear();

        final Object editableValue = getEditableValue();
        IFilter filter;
        if (editableValue instanceof DBSObject) {
            filter = new DataSourcePropertyFilter(((DBSObject) editableValue).getDataSource());
        } else if (editableValue instanceof IDataSourceProvider) {
            filter = new DataSourcePropertyFilter(((IDataSourceProvider) editableValue).getDataSource());
        } else {
            filter = new DataSourcePropertyFilter(null);
        }
        List<ObjectPropertyDescriptor> annoProps = ObjectAttributeDescriptor.extractAnnotations(this, editableValue.getClass(), filter);
        for (final ObjectPropertyDescriptor desc : annoProps) {
            addProperty(desc);
        }
        if (editableValue instanceof IPropertySource) {
            IPropertySource ownPropSource = (IPropertySource) editableValue;
            IPropertyDescriptor[] ownProperties = ownPropSource.getPropertyDescriptors();
            if (!ArrayUtils.isEmpty(ownProperties)) {
                for (IPropertyDescriptor prop : ownProperties) {
                    props.add(prop);
                    propValues.put(prop.getId(), ownPropSource.getPropertyValue(prop.getId()));
                }
            }
        }
        return !props.isEmpty();
    }

    private class PropertySheetLoadService extends AbstractLoadService<Map<ObjectPropertyDescriptor, Object>> {

        public static final String TEXT_LOADING = "...";

        public PropertySheetLoadService()
        {
            super(TEXT_LOADING);
        }

        @Override
        public Map<ObjectPropertyDescriptor, Object> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                Map<ObjectPropertyDescriptor, Object> result = new IdentityHashMap<ObjectPropertyDescriptor, Object>();
                for (ObjectPropertyDescriptor prop : obtainLazyProperties()) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
                    result.put(prop, prop.readValue(getEditableValue(), getProgressMonitor()));
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

        @Override
        public Object getFamily() {
            final Object editableValue = getEditableValue();
            return editableValue instanceof DBSObject ? ((DBSObject) editableValue).getDataSource() : editableValue;
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
        //private int callCount = 0;
        private boolean completed = false;

        private PropertySheetLoadVisualizer()
        {
        }

        @Override
        public Shell getShell() {
            return UIUtils.getActiveShell();
        }

        @Override
        public DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor)
        {
            return monitor;
        }

        @Override
        public boolean isCompleted()
        {
            return completed;
        }

        @Override
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

        @Override
        public void completeLoading(Map<ObjectPropertyDescriptor, Object> result)
        {
            completed = true;
            if (result != null) {
                for (Map.Entry<ObjectPropertyDescriptor, Object> entry : result.entrySet()) {
                    lazyValues.put(entry.getKey().getId(), entry.getValue());
                    PropertiesContributor.getInstance().notifyPropertyLoad(getEditableValue(), entry.getKey(), entry.getValue(), true);
                }
            }
        }

    }
}