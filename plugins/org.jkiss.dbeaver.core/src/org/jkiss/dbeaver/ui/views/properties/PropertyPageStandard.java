/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.views.ViewsPlugin;
import org.eclipse.ui.views.properties.*;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

import java.util.*;

public class PropertyPageStandard extends PropertySheetPage implements ILazyPropertyLoadListener, IPropertySourceProvider {

    private static class PropertySourceCache {
        Object object;
        IPropertySource propertySource;
        boolean cached;

        public PropertySourceCache(Object object)
        {
            this.object = object;
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
        if (!CommonUtils.isEmpty(curSelection)) {
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