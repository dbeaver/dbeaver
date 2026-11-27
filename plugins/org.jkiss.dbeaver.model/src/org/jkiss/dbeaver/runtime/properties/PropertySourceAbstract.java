/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyManager;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PropertyCollector
 */
public abstract class PropertySourceAbstract implements DBPPropertyManager, IPropertySourceMulti
{
    private static final Log log = Log.getLog(PropertySourceAbstract.class);
    private static final Object LAZY_VALUE_NOT_LOADED = new Object();

    private final Object sourceObject;
    private final Object object;
    private final boolean loadLazyProps;
    private final List<DBPPropertyDescriptor> props = new ArrayList<>();
    private Map<DBPPropertyDescriptor, Object> changedPropertiesValues = new HashMap<>();
    private final Map<Object, Object> propValues = new HashMap<>();
    private final Map<Object, Object> lazyValues = new HashMap<>();
    private final Object lazyLoadLock = new Object();
    @Nullable
    private volatile PropertyValueLoadService lazyLoadService;
    private String locale;
    private boolean enableFilters = true;

    /**
     * constructs property source
     * @param object object
     */
    public PropertySourceAbstract(Object sourceObject, Object object, boolean loadLazyProps) {
        this.sourceObject = sourceObject;
        this.object = object;
        this.loadLazyProps = loadLazyProps;
    }

    public synchronized void addProperty(@NotNull DBPPropertyDescriptor prop) {
        if (prop instanceof ObjectPropertyDescriptor opd && opd.isHidden()) {
            // Do not add it to property list
        } else {
            props.add(prop);
        }
        propValues.put(prop.getId(), prop);
    }

    public synchronized void addProperty(@Nullable String category, String id, String name, Object value)
    {
        props.add(new PropertyDescriptor(category, id, name, null, value == null ? null : value.getClass(), false, null, null, false));
        propValues.put(id, value);
    }

    public synchronized void removeProperty(DBPPropertyDescriptor prop) {
        propValues.remove(prop.getId());
        lazyValues.remove(prop.getId());
        props.remove(prop);
    }

    public synchronized void clearProperties() {
        props.clear();
        propValues.clear();
        lazyValues.clear();
    }

    public synchronized boolean hasProperty(ObjectPropertyDescriptor prop)
    {
        return props.contains(prop);
    }

    public synchronized boolean isEmpty()
    {
        return props.isEmpty();
    }

    public DBPPropertyDescriptor getProperty(String id) {
        for (DBPPropertyDescriptor prop : props) {
            if (prop.getId().equals(id)) {
                return prop;
            }
        }
        return null;
    }

    public Object getSourceObject()
    {
        return sourceObject;
    }

    @NotNull
    @Override
    public Object getEditableValue()
    {
        return object;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getProperties() {
        return props.toArray(new DBPPropertyDescriptor[0]);
    }

    @Override
    public boolean isPropertySet(@NotNull String id) {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor opd) {
            return isPropertySet(getEditableValue(), opd);
        } else {
            return value != null;
        }
    }

    @Override
    public boolean isPropertySet(@NotNull Object object, @NotNull ObjectPropertyDescriptor prop) {
        try {
            return !prop.isLazy(object, true) && prop.readValue(object, null, false) != null;
        } catch (Exception e) {
            log.error("Error reading property '" + prop.getId() + "' from " + object, e);
            return false;
        }
    }

    @Nullable
    @Override
    public final Object getPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull final String id) {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor opd) {
            Object editableValue = getEditableValue();
            value = editableValue == null ? null : getPropertyValue(monitor, editableValue, opd, true);
        }
        return value;
    }


    @Override
    public Object getPropertyValue(
        @Nullable DBRProgressMonitor monitor,
        @NotNull Object object,
        @NotNull ObjectPropertyDescriptor prop,
        boolean formatValue
    ) {
        try {
            if (monitor == null && prop.isLazy(object, true) && !prop.supportsPreview()) {
                synchronized (lazyLoadLock) {
                    if (getEditableValue() != object) {
                        // Dynamic lists may ask for an unselected row while painting it.
                        return null;
                    }
                    if (lazyLoadService != null && lazyLoadService.target != object) {
                        // A list property source can be reused for another selection while the old job is still running.
                        lazyValues.clear();
                        lazyLoadService = null;
                    }
                    if (lazyValues.containsKey(prop.getId())) {
                        // A loaded property (or a failed read) can have a null value.
                        return lazyValues.get(prop.getId());
                    }
                    if (!loadLazyProps) {
                        return null;
                    }
                    DBPDataSource dataSource = DBUtils.getObjectDataSource(object);
                    if (!isLazyLoadAvailable(object, dataSource)) {
                        return null;
                    }
                    if (lazyLoadService == null) {
                        lazyLoadService = new PropertyValueLoadService(object, dataSource);
                    }
                    lazyLoadService.schedule(prop);
                }
                return null;
            } else {
                return prop.readValue(object, monitor, formatValue);
            }
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException ite) {
                e = ite.getTargetException();
            }
            log.error("Error reading property '" + prop.getId() + "' from " + object, e);
            return e.getMessage();
        }
    }

    @Override
    public boolean isPropertyResettable(@NotNull String id) {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor opd) {
            return isPropertyResettable(getEditableValue(), opd);
        } else {
            // No by default
            return false;
        }
    }

    @Override
    public boolean isPropertyResettable(@NotNull Object object, @NotNull ObjectPropertyDescriptor prop)
    {
        return false;
    }

    @Override
    public final void resetPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id) {
        Object value = propValues.get(id);
        if (value instanceof ObjectPropertyDescriptor) {
            resetPropertyValue(monitor, getEditableValue(), (ObjectPropertyDescriptor) value);
        } else {
            throw new UnsupportedOperationException("Direct property reset not implemented");
        }
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull Object object, @NotNull ObjectPropertyDescriptor id) {
        throw new UnsupportedOperationException("Cannot reset property in non-editable property source");
    }

    @Override
    public void resetPropertyValueToDefault(@NotNull String id) {
        throw new UnsupportedOperationException("Cannot reset property in non-editable property source");
    }

    @Override
    public final void setPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id, @Nullable Object value)
    {
        Object prop = propValues.get(id);
        if (prop instanceof ObjectPropertyDescriptor) {
            setPropertyValue(monitor, getEditableValue(), (ObjectPropertyDescriptor) prop, value);
            lazyValues.put(((ObjectPropertyDescriptor) prop).getId(), value);
        } else {
            propValues.put(id, value);
        }
    }

    @Override
    public void setPropertyValue(
        @Nullable DBRProgressMonitor monitor,
        @NotNull Object object,
        @NotNull ObjectPropertyDescriptor prop,
        @Nullable Object value
    ) {
        throw new UnsupportedOperationException("Cannot update property in non-editable property source");
    }

    public boolean collectProperties() {
        return collectProperties(true);
    }

    public boolean collectProperties(boolean collectLocalizedNames) {
        lazyValues.clear();
        props.clear();
        propValues.clear();

        final Object editableValue = getEditableValue();
        if (editableValue != null) {
            IPropertyFilter filter;
            if (isEnableFilters()) {
                if (editableValue instanceof DBSObject) {
                    filter = new DataSourcePropertyFilter(((DBSObject) editableValue).getDataSource());
                } else if (editableValue instanceof DBPContextProvider) {
                    DBCExecutionContext context = ((DBPContextProvider) editableValue).getExecutionContext();
                    filter = context == null ? new DataSourcePropertyFilter() : new DataSourcePropertyFilter(context.getDataSource());
                } else {
                    filter = new DataSourcePropertyFilter();
                }
            } else {
                filter = null;
            }
            List<ObjectPropertyDescriptor> annoProps = ObjectAttributeDescriptor.extractAnnotations(
                this,
                ObjectPropertyDescriptor.getObjectClass(editableValue),
                filter,
                locale,
                collectLocalizedNames
            );
            for (final ObjectPropertyDescriptor desc : annoProps) {
                if (desc.isPropertyVisible(editableValue, editableValue)) {
                    addProperty(desc);
                }
            }
            if (editableValue instanceof DBPPropertySource ownPropSource) {
                DBPPropertyDescriptor[] ownProperties = ownPropSource.getProperties();
                if (!ArrayUtils.isEmpty(ownProperties)) {
                    for (DBPPropertyDescriptor prop : ownProperties) {
                        props.add(prop);
                        propValues.put(prop.getId(), ownPropSource.getPropertyValue(null, prop.getId()));
                    }
                }
            }
        }
        return !props.isEmpty();
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isEnableFilters() {
        return enableFilters;
    }

    public void setEnableFilters(boolean enableFilters) {
        this.enableFilters = enableFilters;
    }

    public boolean getEnableFilters() {
        return enableFilters;
    }

    public void addChangedProperties(DBPPropertyDescriptor descriptor, Object newValue) {
        changedPropertiesValues.put(descriptor, newValue);
    }

    public void setChangedPropertiesMap(Map<DBPPropertyDescriptor, Object> newMap) {
        this.changedPropertiesValues = newMap;
    }

    @Nullable
    public Map<DBPPropertyDescriptor, Object> getChangedPropertiesValues() {
        return changedPropertiesValues;
    }

    private boolean isLazyLoadAvailable(@NotNull Object target, @Nullable DBPDataSource dataSource) {
        // List property sources follow the selection, which can disappear while loading is pending.
        if (getEditableValue() != target) {
            return false;
        }
        if (dataSource == null) {
            return true;
        }
        DBPDataSourceContainer container = dataSource.getContainer();
        // An old editor can still have queued property reads after its connection has been replaced.
        return container.isConnected() && !container.isConnecting() && container.getDataSource() == dataSource;
    }

    private class PropertyValueLoadService extends AbstractLoadService<Map<ObjectPropertyDescriptor, Object>> {
        private final Deque<List<ObjectPropertyDescriptor>> completedBatches = new ArrayDeque<>();
        private final Object target;
        @Nullable
        private final DBPDataSource dataSource;
        private final List<ObjectPropertyDescriptor> lazyProps = new ArrayList<>();
        private final Set<Object> pendingLazyValues = new HashSet<>();
        private Job lazyLoadJob;

        private PropertyValueLoadService(@NotNull Object target, @Nullable DBPDataSource dataSource) {
            super(ModelMessages.model_navigator_load_);
            this.target = target;
            this.dataSource = dataSource;
        }

        private boolean isAvailable() {
            return lazyLoadService == this && isLazyLoadAvailable(target, dataSource);
        }

        private void schedule(@NotNull ObjectPropertyDescriptor prop) {
            if (!pendingLazyValues.add(prop.getId())) {
                return;
            }
            lazyProps.add(prop);
            if (lazyLoadJob == null) {
                lazyLoadJob = DBWorkbench.getPlatformUI().createLoadingService(this, new PropertyValueLoadVisualizer(this));
                lazyLoadJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(@NotNull IJobChangeEvent event) {
                        synchronized (lazyLoadLock) {
                            if (!isAvailable() || event.getResult().matches(IStatus.CANCEL)) {
                                for (ObjectPropertyDescriptor pending : lazyProps) {
                                    pendingLazyValues.remove(pending.getId());
                                }
                                lazyProps.clear();
                            }
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

        @NotNull
        @Override
        public Map<ObjectPropertyDescriptor, Object> evaluate(@NotNull DBRProgressMonitor monitor)
            throws InvocationTargetException
        {
            Map<ObjectPropertyDescriptor, Object> result = new IdentityHashMap<>();
            List<ObjectPropertyDescriptor> properties = obtainLazyProperties();
            for (ObjectPropertyDescriptor prop : properties) {
                result.put(prop, LAZY_VALUE_NOT_LOADED);
            }
            try {
                for (ObjectPropertyDescriptor prop : properties) {
                    if (monitor.isCanceled() || !isAvailable()) {
                        break;
                    }
                    result.put(prop, prop.readValue(target, monitor, true));
                }
                return result;
            } catch (Throwable ex) {
                if (monitor.isCanceled() || !isAvailable()) {
                    return result;
                }
                if (ex instanceof InvocationTargetException ite) {
                    throw ite;
                } else {
                    throw new InvocationTargetException(ex);
                }
            } finally {
                synchronized (lazyLoadLock) {
                    // UI completions are queued in run order, including failed runs with a null result.
                    completedBatches.addLast(properties);
                }
            }
        }

        @NotNull
        @Override
        public Object getFamily() {
            // JobManager queries families during disconnect, independently of the current UI selection.
            return dataSource == null ? target : dataSource;
        }

        @NotNull
        private List<ObjectPropertyDescriptor> obtainLazyProperties() {
            synchronized (lazyLoadLock) {
                List<ObjectPropertyDescriptor> result = new ArrayList<>(lazyProps);
                lazyProps.clear();
                return result;
            }
        }
    }

    private class PropertyValueLoadVisualizer implements ILoadVisualizer<Map<ObjectPropertyDescriptor, Object>> {
        //private Object propertyId;
        //private int callCount = 0;
        private boolean completed = false;
        private final PropertyValueLoadService service;

        private PropertyValueLoadVisualizer(@NotNull PropertyValueLoadService service) {
            this.service = service;
        }

        @NotNull
        @Override
        public DBRProgressMonitor overwriteMonitor(@NotNull DBRProgressMonitor monitor)
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
            propValues.put(propertyId, PropertyValueLoadService.TEXT_LOADING + dots);
            refreshProperties(false);
*/
        }

        @Override
        public void completeLoading(@Nullable Map<ObjectPropertyDescriptor, Object> result)
        {
            completed = true;
            synchronized (lazyLoadLock) {
                List<ObjectPropertyDescriptor> properties = service.completedBatches.pollFirst();
                if (service.isAvailable()) {
                    if (result == null && properties != null) {
                        // Ordinary failures must not be retried by painting the property sheet.
                        // An explicit property refresh clears this cache and permits another attempt.
                        for (ObjectPropertyDescriptor prop : properties) {
                            lazyValues.putIfAbsent(prop.getId(), null);
                        }
                    } else if (result != null) {
                        // Listeners can synchronously reread the entire property sheet.
                        // Publish the whole batch before releasing pending markers or notifying them.
                        for (Map.Entry<ObjectPropertyDescriptor, Object> entry : result.entrySet()) {
                            if (entry.getValue() != LAZY_VALUE_NOT_LOADED) {
                                lazyValues.put(entry.getKey().getId(), entry.getValue());
                            }
                        }
                    }
                }
                if (properties != null) {
                    for (ObjectPropertyDescriptor prop : properties) {
                        service.pendingLazyValues.remove(prop.getId());
                    }
                }
            }
            if (result != null) {
                for (Map.Entry<ObjectPropertyDescriptor, Object> entry : result.entrySet()) {
                    if (entry.getValue() != LAZY_VALUE_NOT_LOADED && service.isAvailable()) {
                        PropertiesContributor.getInstance().notifyPropertyLoad(service.target, entry.getKey(), entry.getValue(), true);
                    }
                }
            }
        }

    }
}
