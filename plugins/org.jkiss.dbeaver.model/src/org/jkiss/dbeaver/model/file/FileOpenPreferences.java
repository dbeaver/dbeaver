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
package org.jkiss.dbeaver.model.file;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A missing or unavailable saved action means that the user must choose an action.
 */
public class FileOpenPreferences {
    private static final String ACTION_PREFIX = "file.open.action.";

    private final DBPPreferenceStore store;
    private final String preferenceKey;

    public FileOpenPreferences(@NotNull DBPPreferenceStore store, @NotNull String handlerId) {
        this.store = store;
        this.preferenceKey = ACTION_PREFIX + handlerId;
    }

    @Nullable
    public FileTypeAction getSavedAction(@NotNull Collection<FileTypeAction> availableActions) {
        String savedAction = store.getString(preferenceKey);
        for (FileTypeAction action : availableActions) {
            if (action.name().equals(savedAction)) {
                return action;
            }
        }
        return null;
    }

    public void setSavedAction(@Nullable FileTypeAction action) {
        if (action == null) {
            store.setToDefault(preferenceKey);
        } else {
            store.setValue(preferenceKey, action.name());
        }
    }

    @NotNull
    public static List<FileTypeAction> getAvailableActions(@NotNull Set<FileTypeAction> actions) {
        return actions.stream()
            .sorted()
            .toList();
    }
}
