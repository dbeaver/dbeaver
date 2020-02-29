/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLFormatterTokenized;
import org.jkiss.dbeaver.utils.PrefUtils;

public class SQLModelPreferencesInitializer extends AbstractPreferenceInitializer {

    public SQLModelPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        // Common
        PrefUtils.setDefaultPreferenceValue(store, SQLModelPreferences.SQL_FORMAT_FORMATTER, SQLFormatterTokenized.FORMATTER_ID);
        PrefUtils.setDefaultPreferenceValue(store, SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS, true);
    }

}
