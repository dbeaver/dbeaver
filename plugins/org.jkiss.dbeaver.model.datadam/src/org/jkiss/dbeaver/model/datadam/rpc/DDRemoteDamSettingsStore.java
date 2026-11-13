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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Last settings delivered to this desktop session. Null means the session is unavailable. */
public class DDRemoteDamSettingsStore {
    private static final Log log = Log.getLog(DDRemoteDamSettingsStore.class);
    private static final DDRemoteDamSettingsStore INSTANCE = new DDRemoteDamSettingsStore();

    private final AtomicReference<DDRemoteDamSettings> settings = new AtomicReference<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private DDRemoteDamSettingsStore() {
    }

    @NotNull
    public static DDRemoteDamSettingsStore getInstance() {
        return INSTANCE;
    }

    @Nullable
    public DDRemoteDamSettings getSettings() {
        return settings.get();
    }

    /** Notifies immediately and on changes; later notifications run on the update thread. */
    public void addListener(@NotNull Listener listener) {
        listeners.add(listener);
        notifyListener(listener, settings.get());
    }

    public void removeListener(@NotNull Listener listener) {
        listeners.remove(listener);
    }

    public void updateSettings(@Nullable DDRemoteDamSettings updated) {
        if (!Objects.equals(settings.getAndSet(updated), updated)) {
            for (Listener listener : listeners) {
                notifyListener(listener, updated);
            }
        }
    }

    private void notifyListener(@NotNull Listener listener, @Nullable DDRemoteDamSettings updated) {
        try {
            listener.settingsChanged(updated);
        } catch (Exception e) {
            log.error("Cannot notify DataDam settings listener", e);
        }
    }

    /** Receives the current settings and later updates; {@code null} means the session is unavailable. */
    @FunctionalInterface
    public interface Listener {
        void settingsChanged(@Nullable DDRemoteDamSettings settings) throws DBException;
    }
}
