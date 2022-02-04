/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AbstractDBASessionPersistence implements DBASessionPersistence {
    protected final Map<String, Object> attributes = new HashMap<>();
    protected final Map<String, Function<Object, Object>> attributeDisposers = new HashMap<>();

    @Override
    public Map<String, Object> getAttributes() {
        synchronized (attributes) {
            return new HashMap<>(attributes);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name) {
        synchronized (attributes) {
            Object value = attributes.get(name);
            if (value instanceof PersistentAttribute) {
                value = ((PersistentAttribute) value).getValue();
            }
            return (T) value;
        }
    }

    @Override
    public <T> T getAttribute(String name, Function<T, T> creator, Function<T, T> disposer) {
        synchronized (attributes) {
            Object value = attributes.get(name);
            if (value instanceof PersistentAttribute) {
                value = ((PersistentAttribute) value).getValue();
            }
            if (value == null) {
                value = creator.apply(null);
                if (value != null) {
                    attributes.put(name, value);
                    if (disposer != null) {
                        attributeDisposers.put(name, (Function<Object, Object>) disposer);
                    }
                }
            }
            return (T) value;
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        setAttribute(name, value, false);
    }

    @Override
    public void setAttribute(String name, Object value, boolean persistent) {
        synchronized (attributes) {
            if (persistent) {
                value = new PersistentAttribute(value);
            }
            attributes.put(name, value);
        }
    }

    @Override
    public Object removeAttribute(String name) {
        synchronized (attributes) {
            return attributes.remove(name);
        }
    }
}
