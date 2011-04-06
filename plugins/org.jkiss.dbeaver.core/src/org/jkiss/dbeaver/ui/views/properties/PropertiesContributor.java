/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertiesContributor
 */
public class PropertiesContributor {

    public static final String CONTRIBUTOR_ID = "org.jkiss.dbeaver.core.propertyViewContributor";

    public static final String CATEGORY_INFO = "Information";
    public static final String CATEGORY_STRUCT = "Structure";

    public static final String TAB_STANDARD = "standard";

    public static final String SECTION_STANDARD = "standard";

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
