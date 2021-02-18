/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPGlobalEventListener;
import org.jkiss.dbeaver.model.app.DBPGlobalEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GlobalEventManagerImpl
 */
public class GlobalEventManagerImpl implements DBPGlobalEventManager {

    private static final Log log = Log.getLog(GlobalEventManagerImpl.class);

    private static GlobalEventManagerImpl instance;

    private final List<DBPGlobalEventListener> listeners = new ArrayList<>();

    public static GlobalEventManagerImpl getInstance() {
        if (instance == null) {
            instance = new GlobalEventManagerImpl();
        }
        return instance;
    }

    @NotNull
    private DBPGlobalEventListener[] getListenersCopy() {
        DBPGlobalEventListener[] listeners;
        synchronized (this.listeners) {
            listeners = this.listeners.toArray(new DBPGlobalEventListener[0]);
        }
        return listeners;
    }

    @Override
    public void fireGlobalEvent(String eventId, Map<String, Object> properties) {
        for (DBPGlobalEventListener listener : getListenersCopy()) {
            listener.handleGlobalEvent(eventId, properties);
        }
    }

    @Override
    public void addEventListener(DBPGlobalEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeEventListener(DBPGlobalEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}
