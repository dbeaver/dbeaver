/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

/**
 * PropertyCollector
 */
public class PropertyCollector extends PropertySourceAbstract
{
    public PropertyCollector(Object sourceObject, Object object, boolean loadLazyProps)
    {
        super(sourceObject, object, loadLazyProps);
    }

    public PropertyCollector(Object object, boolean loadLazyProps)
    {
        super(object, object, loadLazyProps);
    }

}
