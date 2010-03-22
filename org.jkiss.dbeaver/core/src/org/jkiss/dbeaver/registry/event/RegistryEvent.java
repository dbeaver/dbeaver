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
