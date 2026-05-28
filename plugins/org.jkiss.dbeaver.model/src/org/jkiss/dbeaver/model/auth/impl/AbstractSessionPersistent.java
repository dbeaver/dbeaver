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
package org.jkiss.dbeaver.model.auth.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.function.ThrowableConsumer;
import org.jkiss.utils.function.ThrowableFunction;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSessionPersistent implements SMSessionPersistent {
    private static final Log log = Log.getLog(AbstractSessionPersistent.class);

    protected final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, ThrowableConsumer<Object, Exception>> attributeDisposers = new HashMap<>();

    @NotNull
    @Override
    public Map<String, Object> getAttributes() {
        synchronized (attributes) {
            return new HashMap<>(attributes);
        }
    }


    @Override
    public <T> T getAttribute(@NotNull String name) {
        synchronized (attributes) {
            Object value = attributes.get(name);
            if (value instanceof PersistentAttribute persistentAttribute) {
                value = persistentAttribute.value();
            }
            return (T) value;
        }
    }

    @Override
    public void setAttribute(@NotNull String name, @Nullable Object value) {
        synchronized (attributes) {
            attributes.put(name, value);
        }
    }

    @Nullable
    @Override
    public Object removeAttribute(@NotNull String name) {
        synchronized (attributes) {
            return attributes.remove(name);
        }
    }

    public void setAttribute(@NotNull String name, @Nullable Object value, boolean persistent) {
        synchronized (attributes) {
            attributes.put(name, persistent ? new PersistentAttribute(value) : value);
        }
    }

    @Override
    @NotNull
    public <T, E extends Exception> T getAttribute(
        @NotNull String name,
        @NotNull ThrowableFunction<String, T, E> creator,
        @Nullable ThrowableConsumer<T, E> disposer
    ) throws E {
        synchronized (attributes) {
            Object value = attributes.get(name);
            if (value instanceof PersistentAttribute persistentAttribute) {
                value = persistentAttribute.value();
            }
            if (value == null) {
                value = creator.apply(null);
                if (value != null) {
                    attributes.put(name, value);
                    if (disposer != null) {
                        attributeDisposers.put(name, (ThrowableConsumer<Object, Exception>) disposer);
                    }
                }
            }
            return (T) value;
        }
    }

    protected void resetSessionCache() throws DBCException {
        // Clear attributes
        synchronized (attributes) {
            for (Map.Entry<String, ThrowableConsumer<Object, Exception>> attrDisposer : attributeDisposers.entrySet()) {
                Object attrValue = attributes.get(attrDisposer.getKey());

                try {
                    attrDisposer.getValue().accept(attrValue);
                } catch (Exception e) {
                    log.error("Error disposing attribute '" + attrDisposer.getKey() + "'", e);
                }
            }
            attributeDisposers.clear();
            // Remove all non-persistent attributes
            attributes.entrySet().removeIf(
                entry -> !(entry.getValue() instanceof PersistentAttribute));
        }
    }

    @Override
    public void close() {
        try {
            resetSessionCache();
        } catch (DBCException e) {
            log.error("Error cleaning up session cache", e);
        }
        this.attributes.clear();
    }

    private record PersistentAttribute(Object value) {
    }

}
