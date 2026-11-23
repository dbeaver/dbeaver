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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPRegistryListener;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;

import java.util.IdentityHashMap;
import java.util.Map;

/** Cached number of AI-enabled connections, readable without opening a project. */
public final class AIProjectConnectionCount implements DBPRegistryListener {
    private static final Log log = Log.getLog(AIProjectConnectionCount.class);
    private static final String PROJECT_PROPERTY = "ai.enabledConnectionCount";
    private static final AIProjectConnectionCount INSTANCE = new AIProjectConnectionCount();

    private final Map<DBPDataSourceRegistry, DBPEventListener> listeners = new IdentityHashMap<>();
    private boolean started;

    private AIProjectConnectionCount() {
    }

    @NotNull
    public static AIProjectConnectionCount getInstance() {
        return INSTANCE;
    }

    @Nullable
    public static Integer getCount(@NotNull DBPProject project) {
        Object value = project.getProjectProperty(PROJECT_PROPERTY);
        return value instanceof Number number && number.intValue() >= 0 ? number.intValue() : null;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        DataSourceProviderRegistry.getInstance().addDataSourceRegistryListener(this);
        for (DBPDataSourceRegistry registry : DBUtils.getAllRegistries(false)) {
            handleRegistryLoad(registry);
        }
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;
        DataSourceProviderRegistry.getInstance().removeDataSourceRegistryListener(this);
        for (var entry : listeners.entrySet()) {
            entry.getKey().removeDataSourceListener(entry.getValue());
        }
        listeners.clear();
    }

    @Override
    public synchronized void handleRegistryLoad(@NotNull DBPDataSourceRegistry registry) {
        if (listeners.containsKey(registry)) {
            return;
        }
        DBPEventListener listener = event -> refresh(registry);
        listeners.put(registry, listener);
        registry.addDataSourceListener(listener);
        refresh(registry);
    }

    @Override
    public synchronized void handleRegistryUnload(@NotNull DBPDataSourceRegistry registry) {
        DBPEventListener listener = listeners.remove(registry);
        if (listener != null) {
            registry.removeDataSourceListener(listener);
        }
    }

    public static void refresh(@NotNull DBPDataSourceRegistry registry) {
        try {
            DBPProject project = registry.getProject();
            int count = (int) registry.getDataSources().stream()
                .filter(connection -> !connection.isTemporary())
                .filter(connection -> new AIContextSettingsDataSource(connection).isMetaTransferConfirmed())
                .count();
            if (!Integer.valueOf(count).equals(getCount(project))) {
                project.setProjectProperty(PROJECT_PROPERTY, count);
            }
        } catch (RuntimeException e) {
            log.debug("Cannot update AI connection count", e);
        }
    }
}
