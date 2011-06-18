/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.*;
import org.jkiss.dbeaver.ui.properties.ILazyPropertyLoadListener;

import java.util.Iterator;

public class PropertyPageStandard extends PropertySheetPage implements ILazyPropertyLoadListener, IPropertySourceProvider {

    private static class PropertySourceCache {
        Object object;
        IPropertySource propertySource;
        boolean cached;

        public PropertySourceCache(Object object)
        {
            if (object instanceof IPropertySource) {
                // Sometimes IPropertySource wrapper (e.g. PropertySourceEditable) may be used instead of real object
                this.object = ((IPropertySource)object).getEditableValue();
            } else {
                this.object = object;
            }
        }
    }
    private PropertySourceCache[] curSelection = null;

    public PropertyPageStandard()
    {
        setSorter(
            new PropertySheetSorter() {
                public int compare(IPropertySheetEntry entryA, IPropertySheetEntry entryB)
                {
                    // No damn sorting
                    return 0;
                }
            }
        );
        setPropertySourceProvider(this);
        // Register lazy load listener
        PropertiesContributor.getInstance().addLazyListener(this);
    }

    @Override
    public void dispose()
    {
        // Unregister lazy load listener
        PropertiesContributor.getInstance().removeLazyListener(this);
        super.dispose();
    }

    public void handlePropertyLoad(Object object, Object propertyId, Object propertyValue, boolean completed)
    {
        // Make page refresh if our main object was updated
        if (!CommonUtils.isEmpty(curSelection) && !getControl().isDisposed()) {
            for (PropertySourceCache cache : curSelection) {
                if (cache.object == object) {
                    refresh();
                    return;
                }
            }
        }
        //System.out.println("HEY: " + object + " | " + propertyId + " | " + propertyValue + " : " + completed);
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        // Create objects cache
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            curSelection = new PropertySourceCache[ss.size()];
            if (ss.size() == 1) {
                curSelection[0] = new PropertySourceCache(ss.getFirstElement());
            } else {
                int index = 0;
                for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
                    curSelection[index++] = new PropertySourceCache(iter.next());
                }
            }
        }
        super.selectionChanged(part, selection);
    }

    public IPropertySource getPropertySource(Object object)
    {
        if (object == null || object.getClass().isPrimitive() || object instanceof CharSequence || object instanceof Number || object instanceof Boolean) {
            // Just for better performance
            return null;
        }
        // Seek in cached property sources
        // Without cache we'll fall in infinite recursion when refreshing lazy props
        // (get prop source from adapter, load props, load lazy props -> refresh -> get prop source from adapter, etc).
        if (!CommonUtils.isEmpty(curSelection)) {
            for (PropertySourceCache cache : curSelection) {
                if (cache.object == object) {
                    if (!cache.cached) {
                        cache.propertySource = (IPropertySource) Platform.getAdapterManager().getAdapter(object, IPropertySource.class);
                        cache.cached = true;
                    }
                    return cache.propertySource;
                }
            }
        }
        return (IPropertySource) Platform.getAdapterManager().getAdapter(object, IPropertySource.class);
    }

}