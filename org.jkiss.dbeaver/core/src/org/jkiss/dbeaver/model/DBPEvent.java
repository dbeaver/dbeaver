/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBPEvent
 */
public class DBPEvent
{
    public enum Action
    {
        OBJECT_ADD,
        OBJECT_UPDATE,
        OBJECT_REMOVE,
    }

    private Action action;

    private DBSObject object;
    private Boolean enabled;
    private Object data;

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
