/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.event;

import org.jkiss.code.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DBFEventManager {
    private static DBFEventManager instance;

    private final List<DBFEventListener> listeners = new CopyOnWriteArrayList<>();

    private DBFEventManager() {
    }

    public synchronized static DBFEventManager getInstance() {
        if (instance == null) {
            instance = new DBFEventManager();
        }
        return instance;
    }


    public void addListener(@NotNull DBFEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(@NotNull DBFEventListener listener) {
        this.listeners.remove(listener);
    }

    public void fireFileSystemEvent(@NotNull DBFEvent event) {
        for (DBFEventListener listener : this.listeners) {
            listener.handleFileSystemEvent(event);
        }
    }
}
