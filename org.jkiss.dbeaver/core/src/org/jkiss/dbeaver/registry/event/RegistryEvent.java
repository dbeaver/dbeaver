/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.event;

import java.util.EventObject;

/**
 * RegistryEvent
 */
public abstract class RegistryEvent extends EventObject
{
    protected RegistryEvent(Object source)
    {
        super(source);
    }
}
