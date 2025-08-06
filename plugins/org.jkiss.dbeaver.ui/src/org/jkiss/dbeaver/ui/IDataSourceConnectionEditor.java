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

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

/**
 * IDataSourceConnectionEditor
 */
public interface IDataSourceConnectionEditor extends IDialogPage {
    /**
     * Sets editor site
     */
    void setSite(@NotNull IDataSourceConnectionEditorSite site);

    /**
     * @return true if all mandatory fields were completed
     */
    boolean isComplete();

    /**
     * @return true if all parameters are provided by some external source.
     *     In this case all mandatory connection parameters become optional (as they could be populated externally).
     */
    default boolean isExternalConfigurationProvided() {
        return false;
    }

    /**
     * Load settings from active datasource info UI
     */
    void loadSettings();

    /**
     * Save all properties info passed datasource
     */
    void saveSettings(DBPDataSourceContainer dataSource);

    // Called once after page activation
    default void activateEditor() {
        Control control = getControl();
        if (control != null) {
            control.setFocus();
        }
    }

}
