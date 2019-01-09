/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

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
                @Override
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

    @Override
    public void handlePropertyLoad(Object object, DBPPropertyDescriptor property, Object propertyValue, boolean completed)
    {
        // Make page refresh if our main object was updated
        if (!ArrayUtils.isEmpty(curSelection) && !getControl().isDisposed()) {
            for (PropertySourceCache cache : curSelection) {
                if ((cache.propertySource != null && cache.propertySource.getEditableValue() == object) ||
                    cache.object == object)
                {
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
        getControl().setRedraw(false);
        try {
            super.selectionChanged(part, selection);
        } finally {
            getControl().setRedraw(true);
        }
    }

    @Override
    public IPropertySource getPropertySource(Object object)
    {
        if (object == null || object.getClass().isPrimitive() || object instanceof CharSequence || object instanceof Number || object instanceof Boolean) {
            // Just for better performance
            return null;
        }
        // Seek in cached property sources
        // Without cache we'll fall in infinite recursion when refreshing lazy props
        // (get prop source from adapter, load props, load lazy props -> refresh -> get prop source from adapter, etc).
        if (!ArrayUtils.isEmpty(curSelection)) {
            for (PropertySourceCache cache : curSelection) {
                if (cache.object == object) {
                    if (!cache.cached) {
                        cache.propertySource = RuntimeUtils.getObjectAdapter(object, IPropertySource.class);
                        cache.cached = true;
                    }
                    return cache.propertySource;
                }
            }
        }
        return RuntimeUtils.getObjectAdapter(object, IPropertySource.class);
    }

}