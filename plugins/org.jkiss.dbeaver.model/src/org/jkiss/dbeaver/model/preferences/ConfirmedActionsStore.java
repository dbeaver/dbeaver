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
package org.jkiss.dbeaver.model.preferences;

import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public final class ConfirmedActionsStore {
    private static final Log log = Log.getLog(ConfirmedActionsStore.class);

    private final Path path;

    public ConfirmedActionsStore(@NotNull Path path) {
        this.path = path;
    }

    public synchronized boolean contains(@NotNull String action) throws DBException {
        return load().contains(action);
    }

    public synchronized boolean add(@NotNull String action) throws DBException {
        Set<String> actions = load();
        boolean changed = actions.add(action);
        if (changed) {
            save(actions);
        }
        return changed;
    }

    public synchronized void replaceWithPrefix(@NotNull String prefix, @Nullable String action) throws DBException {
        Set<String> actions = load();
        boolean changed = actions.removeIf(value -> value.startsWith(prefix) && !value.equals(action));
        if (action != null) {
            changed |= actions.add(action);
        }
        if (changed) {
            save(actions);
        }
    }

    @NotNull
    private Set<String> load() throws DBException {
        if (!Files.exists(path)) {
            return new HashSet<>();
        }
        try {
            String json = Files.readString(path);
            Set<String> actions = JSONUtils.GSON.fromJson(json, new TypeToken<Set<String>>() { }.getType());
            if (actions == null) {
                return new HashSet<>();
            }
            actions.remove(null);
            return actions;
        } catch (IOException exception) {
            throw new DBException("Cannot read local action approvals: " + path, exception);
        } catch (RuntimeException exception) {
            log.error("Invalid local action approvals: " + path, exception);
            return new HashSet<>();
        }
    }

    private void save(@NotNull Set<String> actions) throws DBException {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temporary)) {
                    JSONUtils.PRETTY_GSON.toJson(actions, writer);
                }
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new DBException("Cannot save local action approvals: " + path, exception);
        }
    }
}
