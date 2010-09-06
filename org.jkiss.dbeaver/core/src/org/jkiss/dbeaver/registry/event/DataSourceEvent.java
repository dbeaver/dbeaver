/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.event;

import org.jkiss.dbeaver.model.struct.DBSObject;
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
        INVALIDATE,

        OBJECT_ADD,
        OBJECT_REMOVE,
        OBJECT_CHANGE,
        OBJECT_REFRESH
    }

    private Action action;
    private DataSourceDescriptor dataSource;

    private DBSObject object;
    private Boolean enabled;
    private Object data;

    public DataSourceEvent(Object source, Action action, DataSourceDescriptor dataSource)
    {
        super(source);
        this.action = action;
        this.dataSource = dataSource;
    }

    public DataSourceEvent(Object source, Action action, DBSObject object) {
        super(source);
        this.action = action;
        this.object = object;
    }

    public DataSourceEvent(Object source, Action action, DBSObject object, boolean enabled) {
        super(source);
        this.action = action;
        this.object = object;
        this.enabled = enabled;
    }

    public DataSourceEvent(Object source, Action action, DBSObject object, Object data) {
        super(source);
        this.action = action;
        this.object = object;
        this.data = data;
    }

    public Action getAction()
    {
        return action;
    }

    public DataSourceDescriptor getDataSource()
    {
        return dataSource;
    }

    public DBSObject getObject() {
        return object;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Object getData() {
        return data;
    }

}
