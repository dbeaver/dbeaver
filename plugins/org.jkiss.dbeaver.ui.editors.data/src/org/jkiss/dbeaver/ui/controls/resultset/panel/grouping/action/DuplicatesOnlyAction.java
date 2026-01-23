/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action;

import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;

public class DuplicatesOnlyAction extends Action {

    private final GroupingResultsContainer resultsContainer;

    public DuplicatesOnlyAction(@NotNull GroupingResultsContainer resultsContainer) {
        super(ResultSetMessages.controls_resultset_grouping_show_duplicates_only, Action.AS_CHECK_BOX);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.DUPS_RESTRICTED));
        this.resultsContainer = resultsContainer;
    }

    @Override
    public boolean isChecked() {
        DBPDataSource dataSource = resultsContainer.getDataContainer().getDataSource();
        return dataSource != null && dataSource.getContainer().getPreferenceStore()
            .getBoolean(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY);
    }

    @Override
    public void run() {
        DBPDataSource dataSource = resultsContainer.getDataContainer().getDataSource();
        if (dataSource == null) {
            return;
        }
        dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY, !isChecked());
        try {
            resultsContainer.rebuildGrouping();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                ResultSetMessages.grouping_panel_error_title,
                ResultSetMessages.grouping_panel_error_change_duplicate_presentation_message,
                e
            );
        }
    }
}