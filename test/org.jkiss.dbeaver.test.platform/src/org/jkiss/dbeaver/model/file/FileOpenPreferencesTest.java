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

import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileOpenPreferencesTest {
    private final SimplePreferenceStore store = new SimplePreferenceStore() {
        @Override
        public void save() {
        }
    };
    private final FileOpenPreferences preferences = new FileOpenPreferences(store, "csv");
    private final Set<FileTypeAction> actions = EnumSet.allOf(FileTypeAction.class);

    @Test
    void asksByDefault() {
        assertNull(preferences.getSavedAction(actions));
    }

    @Test
    void remembersActionAcrossInstances() {
        preferences.setSavedAction(FileTypeAction.INTERNAL_EDITOR);
        assertEquals(FileTypeAction.INTERNAL_EDITOR, new FileOpenPreferences(store, "csv").getSavedAction(actions));
    }

    @Test
    void preferencesAreSpecificToHandler() {
        preferences.setSavedAction(FileTypeAction.DATABASE);
        assertNull(new FileOpenPreferences(store, "xml").getSavedAction(actions));
    }

    @Test
    void askingAgainClearsSavedAction() {
        preferences.setSavedAction(FileTypeAction.DATABASE);
        preferences.setSavedAction(null);
        assertNull(preferences.getSavedAction(actions));
    }

    @Test
    void unavailableActionFallsBackToAskingWithoutForgettingChoice() {
        preferences.setSavedAction(FileTypeAction.EXTERNAL_EDITOR);
        assertNull(preferences.getSavedAction(Set.of(FileTypeAction.INTERNAL_EDITOR)));
        assertEquals(FileTypeAction.EXTERNAL_EDITOR, preferences.getSavedAction(actions));
    }

    @Test
    void invalidSavedActionFallsBackToAsking() {
        store.setValue("file.open.action.csv", "UNKNOWN");
        assertNull(preferences.getSavedAction(actions));
    }

    @Test
    void supportedActionsAreSorted() {
        assertEquals(List.of(FileTypeAction.DATABASE, FileTypeAction.INTERNAL_EDITOR, FileTypeAction.EXTERNAL_EDITOR),
            FileOpenPreferences.getAvailableActions(actions));
    }

    @Test
    void onlyHandlerSupportedActionsAreAvailable() {
        assertEquals(List.of(FileTypeAction.INTERNAL_EDITOR),
            FileOpenPreferences.getAvailableActions(Set.of(FileTypeAction.INTERNAL_EDITOR)));
        assertEquals(List.of(), FileOpenPreferences.getAvailableActions(Set.of()));
    }
}
