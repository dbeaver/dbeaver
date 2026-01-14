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
package org.jkiss.dbeaver.ui.editors.sql.preferences.format;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.registry.SQLFormatterDescriptor;

/**
 * SQL Formatter configurator
 */
public interface SQLFormatterConfigurator {

    /**
     * Sets configuration parameters
     */
    void configure(SQLFormatterDescriptor formatterDescriptor, Runnable changeListener);

    void loadSettings(DBPPreferenceStore preferenceStore, boolean useDefaults);

    void saveSettings(DBPPreferenceStore preferenceStore);

    void resetSettings(DBPPreferenceStore preferenceStore);
}
