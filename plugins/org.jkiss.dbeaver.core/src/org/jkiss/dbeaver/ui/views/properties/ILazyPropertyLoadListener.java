/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

/**
 * Lazy properties listener
 */
public interface ILazyPropertyLoadListener {

    void handlePropertyLoad(Object object, Object propertyId, Object propertyValue, boolean completed);

}
