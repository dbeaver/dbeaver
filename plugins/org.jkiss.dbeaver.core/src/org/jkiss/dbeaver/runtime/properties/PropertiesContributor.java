/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertiesContributor
 */
public class PropertiesContributor {

    public static final String CATEGORY_INFO = CoreMessages.ui_properties_category_information;
    public static final String CATEGORY_STRUCT = CoreMessages.ui_properties_category_structure;

    public static final String TAB_STANDARD = "standard"; //$NON-NLS-1$

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
