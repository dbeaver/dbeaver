/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.util.*;
import java.util.function.Consumer;

public class WorkspaceConfigEventManager {
    private static final Object syncRoot = new Object();
    private static final Map<String, Set<Consumer<Object>>> listenersByConfigFile = new HashMap<>();

    /**
     * Subscribe for the event of the workspace configuration file being changed.
     */
    public static void addConfigChangedListener(String configFileName, Consumer<Object> listener) {
        synchronized (syncRoot) {
            // using LinkedHashSet to guarantee listeners invocation order
            listenersByConfigFile.computeIfAbsent(configFileName, x -> new LinkedHashSet<>()).add(listener);
        }
    }

    /**
     * Remove the listener of the workspace configuration file changed event.
     */
    public static void removeConfigChangedListener(String configFileName, Consumer<Object> listener) {
        synchronized (syncRoot) {
            Set<Consumer<Object>> listeners = listenersByConfigFile.get(configFileName);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
    }

    /**
     * Raise workspace configuration file changed event.
     */
    public static void fireConfigChangedEvent(String configFileName) {
        List<Consumer<Object>> listenersList;
        synchronized (syncRoot) {
            Set<Consumer<Object>> listeners = listenersByConfigFile.get(configFileName);
            if (listeners != null) {
                listenersList = List.copyOf(listeners);
            } else {
                listenersList = null;
            }
        }
        if (listenersList != null) {
            for (Consumer<Object> listener : listenersList) {
                listener.accept(null);
            }
        }
    }
}
