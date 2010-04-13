/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.event;

import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * DataSourceEvent
 */
public class DataSourceEvent extends RegistryEvent
{
    public enum Action
    {
        ADD,
        CHANGE,
        REMOVE,
        CONNECT,
        CONNECT_FAIL,
        DISCONNECT,
    }

    private Action action;
    private DataSourceDescriptor dataSource;

    public DataSourceEvent(Object source, Action action, DataSourceDescriptor dataSource)
    {
        super(source);
        this.action = action;
        this.dataSource = dataSource;
    }

    public Action getAction()
    {
        return action;
    }

    public DataSourceDescriptor getDataSource()
    {
        return dataSource;
    }
}
