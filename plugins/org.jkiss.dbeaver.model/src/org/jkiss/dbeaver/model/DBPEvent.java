/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

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
        OBJECT_SELECT,
    }

    public static final Object REORDER = new Object();

    private Action action;

    private DBSObject object;
    private Boolean enabled;
    private Object data;
    private Map<String, Object> options;

    public DBPEvent(Action action, DBSObject object) {
        this.action = action;
        this.object = object;
    }

    public DBPEvent(Action action, DBSObject object, boolean enabled) {
        this.action = action;
        this.object = object;
        this.enabled = enabled;
    }

    public DBPEvent(Action action, DBSObject object, @Nullable Object data) {
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

    @Nullable
    public Object getData() {
        return data;
    }

    @Nullable
    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
