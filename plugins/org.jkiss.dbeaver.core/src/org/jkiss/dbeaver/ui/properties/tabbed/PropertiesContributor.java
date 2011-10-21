/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.properties.ILazyPropertyLoadListener;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertiesContributor
 */
public class PropertiesContributor {

    public static final String CONTRIBUTOR_ID = "org.jkiss.dbeaver.core.propertyViewContributor"; //$NON-NLS-1$

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

    public void notifyPropertyLoad(Object object, Object propertyId, Object propertyValue, boolean completed)
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
