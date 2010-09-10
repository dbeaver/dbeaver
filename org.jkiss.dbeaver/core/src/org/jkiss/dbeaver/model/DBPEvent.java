/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * DBPEvent
 */
public class DBPEvent
{
    public enum Action
    {
        ADD,
        CHANGE,
        REMOVE,
        CONNECT,
        CONNECT_FAIL,
        DISCONNECT,

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

    public DBPEvent(Action action, DataSourceDescriptor dataSource)
    {
        this.action = action;
        this.dataSource = dataSource;
    }

    public DBPEvent(Action action, DBSObject object) {
        this.action = action;
        this.object = object;
    }

    public DBPEvent(Action action, DBSObject object, boolean enabled) {
        this.action = action;
        this.object = object;
        this.enabled = enabled;
    }

    public DBPEvent(Action action, DBSObject object, Object data) {
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
