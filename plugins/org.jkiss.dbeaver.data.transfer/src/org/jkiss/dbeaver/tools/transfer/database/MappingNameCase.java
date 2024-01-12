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
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;

public enum MappingNameCase {

    DEFAULT(0, DTMessages.pref_data_transfer_name_case_default, null),
    UPPER(1, DTMessages.pref_data_transfer_name_case_upper, DBPIdentifierCase.UPPER),
    LOWER(2, DTMessages.pref_data_transfer_name_case_lower, DBPIdentifierCase.LOWER);

    private final int selectionId;
    private final String name;
    private final DBPIdentifierCase identifierCase;

    MappingNameCase(int selectionId, String name, DBPIdentifierCase identifierCase) {
        this.selectionId = selectionId;
        this.name = name;
        this.identifierCase = identifierCase;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public DBPIdentifierCase getIdentifierCase() {
        return identifierCase;
    }

    /**
     * Get name case the values list or DEFAULT
     *
     * @param id int for search
     * @return name case from the list or default one
     */
    @NotNull
    public static MappingNameCase getCaseBySelectionId(int id) {
        for (MappingNameCase value : values()) {
            if (id == value.selectionId) {
                return value;
            }
        }
        return DEFAULT;
    }

    /**
     * Get name case from the preferences or DEFAULT
     *
     * @param dbpPreferenceStore datasource preference store
     * @param store general preference store
     * @return name case from the preferences or default one
     */
    @NotNull
    public static MappingNameCase getCaseFromPreferences(
        @NotNull DBPPreferenceStore dbpPreferenceStore,
        @NotNull DBPPreferenceStore store
    ) {
        int selectionIndex;
        if (dbpPreferenceStore.contains(DTConstants.PREF_NAME_CASE_MAPPING)) {
            selectionIndex = dbpPreferenceStore.getInt(DTConstants.PREF_NAME_CASE_MAPPING);
        } else {
            selectionIndex = store.getInt(DTConstants.PREF_NAME_CASE_MAPPING);
        }
        return getCaseBySelectionId(selectionIndex);
    }
}
