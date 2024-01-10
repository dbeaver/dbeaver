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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;

public enum MappingReplaceMechanism {

    ABSENT(0, DTMessages.pref_data_transfer_replacing_combo_do_not),
    UNDERSCORES(1, DTMessages.pref_data_transfer_replacing_combo_underscores),
    CAMELCASE(2, DTMessages.pref_data_transfer_replacing_combo_camel_case);

    private final int selectionId;
    private final String name;

    MappingReplaceMechanism(int selectionId, String name) {
        this.selectionId = selectionId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Get replace mechanism from the values list or ABSENT
     *
     * @param id int for search
     * @return replace mechanism from the list or default one
     */
    @NotNull
    public static MappingReplaceMechanism getCaseBySelectionId(int id) {
        for (MappingReplaceMechanism value : values()) {
            if (id == value.selectionId) {
                return value;
            }
        }
        return ABSENT;
    }

    /**
     * Get replace mechanism from the preferences or ABSENT
     *
     * @param dbpPreferenceStore datasource preference store
     * @param store general preference store
     * @return replace mechanism from the preferences or default one
     */
    @NotNull
    public static MappingReplaceMechanism getCaseFromPreferences(
        @NotNull DBPPreferenceStore dbpPreferenceStore,
        @NotNull DBPPreferenceStore store
    ) {
        int selectionIndex;
        if (dbpPreferenceStore.contains(DTConstants.PREF_REPLACE_MAPPING)) {
            selectionIndex = dbpPreferenceStore.getInt(DTConstants.PREF_REPLACE_MAPPING);
        } else {
            selectionIndex = store.getInt(DTConstants.PREF_REPLACE_MAPPING);
        }
        return getCaseBySelectionId(selectionIndex);
    }
}
