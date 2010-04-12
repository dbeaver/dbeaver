package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.ui.views.properties.*;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.utils.ViewUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PropertyCollector
 */
public class PropertyCollector implements IPropertySource
{
    private DBPObject object;
    private boolean loadLazyObjects;
    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private Map<Object, Object> propValues = new HashMap<Object, Object>();

    public PropertyCollector(DBPObject object)
    {
        this(object, true);
    }

    public PropertyCollector(DBPObject object, boolean loadLazyObjects)
    {
        this.object = object;
        this.loadLazyObjects = loadLazyObjects;
    }

    public PropertyCollector addProperty(Object id, String name, Object value)
    {
        props.add(new PropertyDescriptor(id, name));
        propValues.put(id, value);
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
        if (value instanceof PropertyAnnoDescriptor) {
            try {
                value = ((PropertyAnnoDescriptor)value).readValue(object);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        if (value instanceof ILoadService) {
            if (loadLazyObjects) {
                final ILoadService loadService = (ILoadService) value;
                String loadText = loadService.getServiceName();
                // We assume that it can be called ONLY by properties viewer
                // So, start lazy loading job to update it after value will be loaded
                LoadingUtils.executeService(
                    loadService,
                    new PropertySheetLoadVisualizer(id, loadText));
                // Return dummy string for now
                return loadText;
            } else {
                return null;
            }
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

    public void collectProperties()
    {
        List<PropertyAnnoDescriptor> annoProps = PropertyAnnoDescriptor.extractAnnotations(object);
        for (PropertyAnnoDescriptor desc : annoProps) {
            props.add(desc);
            propValues.put(desc.getId(), desc);
        }
    }

    private class PropertySheetLoadVisualizer implements ILoadVisualizer {
        private Object propertyId;
        private String loadText;
        private int callCount = 0;
        private boolean completed = false;

        private PropertySheetLoadVisualizer(Object propertyId, String loadText)
        {
            this.propertyId = propertyId;
            this.loadText = loadText;
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
            refreshProperties();
        }
        public void completeLoading(Object result)
        {
            completed = true;
            propValues.put(propertyId, result);
            refreshProperties();
        }
        private void refreshProperties()
        {
            // Here is some kind of dirty hack - use direct casts to propertyshet implementation
            PropertiesPage page = PropertiesPage.getPageByObject(object);
            if (page == null) {
                PropertiesView view = ViewUtils.findView(
                    DBeaverCore.getActiveWorkbenchWindow(),
                    PropertiesView.class);
                if (view != null) {
                    page = view.getCurrentPage();
                }
            }
            if (page != null) {
                Object curObject = page.getCurrentObject();
                // Refresh only if current property sheet object is the same as for collector
                if (curObject == object) {
                    DBeaverCore.getInstance().getPropertiesAdapter().addToCache(object, PropertyCollector.this);
                    try {
                        page.refresh();
                    }
                    finally {
                        DBeaverCore.getInstance().getPropertiesAdapter().removeFromCache(object);
                    }
                }
            }
        }

    }
}
