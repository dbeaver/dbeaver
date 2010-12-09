/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * PropertyCollector
 */
public class PropertySourceAbstract implements IPropertySource
{
    private Object object;
    private boolean loadLazyProps;
    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();
    private Map<Object, Object> propValues = new HashMap<Object, Object>();

    /**
     * constructs property source
     * @param object object
     */
    public PropertySourceAbstract(Object object, boolean loadLazyProps)
    {
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
        if (value instanceof PropertyAnnoDescriptor) {
            try {
                PropertyAnnoDescriptor annoDescriptor = (PropertyAnnoDescriptor) value;
                if (annoDescriptor.isLazy()) {
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

        private PropertyAnnoDescriptor annoDescriptor;
        public PropertySheetLoadService(PropertyAnnoDescriptor annoDescriptor)
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
            {
                // If some view or editor contains properties page for current object - refresh it first
                PropertyPageStandard pageStandard = PropertyPageStandard.getPageByObject(object);
                if (pageStandard != null) {
                    refreshProperties(pageStandard);
                }
            }

            // Find our property page within views (PropertySheet view actually)
            PropertyPageStandard pageStandard = null;
            IViewPart view = ViewUtils.findView(
                DBeaverCore.getActiveWorkbenchWindow(),
                IPageLayout.ID_PROP_SHEET);
            if (view != null && view instanceof PropertySheet) {
                IPage testPage = ((PropertySheet)view).getCurrentPage();
                if (testPage instanceof PropertyPageStandard) {
                    pageStandard = (PropertyPageStandard)testPage;
                } else if (testPage instanceof TabbedPropertySheetPage) {
                    TabbedPropertySheetPage tabbedPage = (TabbedPropertySheetPage)testPage;
                    if (tabbedPage.getCurrentTab() != null) {
                        for (ISection section : tabbedPage.getCurrentTab().getSections()) {
                            if (section instanceof PropertySectionStandard) {
                                pageStandard = ((PropertySectionStandard)section).getPage();
                                break;
                            }
                        }
                    }
                }
            }
            if (pageStandard != null) {
                refreshProperties(pageStandard);
            }
        }

        private void refreshProperties(PropertyPageStandard pageStandard)
        {
            Object curObject = pageStandard.getCurrentObject();
            if (curObject instanceof DBSWrapper) {
                curObject = ((DBSWrapper)curObject).getObject();
            }
            // Refresh only if current property sheet object is the same as for collector
            if (curObject == object) {
                DBeaverCore.getInstance().getNavigatorModel().getNodesAdapter().addToCache(object, PropertySourceAbstract.this);
                try {
                    pageStandard.refresh();
                }
                finally {
                    DBeaverCore.getInstance().getNavigatorModel().getNodesAdapter().removeFromCache(object);
                }
            }
        }

    }
}