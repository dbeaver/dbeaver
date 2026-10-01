/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite object command
 */
public abstract class DBECommandComposite<OBJECT_TYPE extends DBPObject, HANDLER_TYPE extends DBEPropertyHandler<OBJECT_TYPE>>
    extends DBECommandAbstract<OBJECT_TYPE> {

    private final Map<Object, Object> properties = new HashMap<>();

    protected DBECommandComposite(@NotNull OBJECT_TYPE object, @NotNull String title)
    {
        super(object, title);
    }

    @NotNull
    public Map<Object, Object> getProperties()
    {
        return properties;
    }

    public Object getProperty(@NotNull Object id)
    {
        return properties.get(id);
    }

    public boolean hasProperty(@NotNull Object id)
    {
        return properties.containsKey(id);
    }

    public Object getProperty(@NotNull HANDLER_TYPE handler)
    {
        return properties.get(handler.getId());
    }

    public void addPropertyHandler(@NotNull HANDLER_TYPE handler, @NotNull Object value)
    {
        properties.put(handler.getId(), value);
    }

}