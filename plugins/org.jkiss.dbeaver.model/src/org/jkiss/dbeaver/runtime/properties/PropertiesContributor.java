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

package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertiesContributor
 */
public class PropertiesContributor {

    public static final String TAB_STANDARD = "standard"; //$NON-NLS-1$
    public static final String TAB_PROPERTIES = "properties"; //$NON-NLS-1$

    public static final String SECTION_STANDARD = "standard"; //$NON-NLS-1$
    public static final String SECTION_ADDITIONAL = "additional"; //$NON-NLS-1$

    private static final PropertiesContributor instance = new PropertiesContributor();

    public static PropertiesContributor getInstance()
    {
        return instance;
    }

    private final List<ILazyPropertyLoadListener> lazyListeners = new ArrayList<ILazyPropertyLoadListener>();

    public void addLazyListener(ILazyPropertyLoadListener listener)
    {
        synchronized (lazyListeners) {
            lazyListeners.add(listener);
        }
    }

    public void removeLazyListener(ILazyPropertyLoadListener listener)
    {
        synchronized (lazyListeners) {
            lazyListeners.remove(listener);
        }
    }

    public void notifyPropertyLoad(Object object, DBPPropertyDescriptor propertyId, Object propertyValue, boolean completed)
    {
        synchronized (lazyListeners) {
            if (!lazyListeners.isEmpty()) {
                for (ILazyPropertyLoadListener listener : lazyListeners) {
                    listener.handlePropertyLoad(object, propertyId, propertyValue, completed);
                }
            }
        }
    }

}
