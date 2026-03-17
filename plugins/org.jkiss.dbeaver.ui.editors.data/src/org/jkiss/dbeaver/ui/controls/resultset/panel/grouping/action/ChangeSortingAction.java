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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;
import org.jkiss.utils.CommonUtils;

public class ChangeSortingAction extends Action {
    private final Boolean descending;
    private final GroupingResultsContainer resultsContainer;

    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String NONE = "";

    @NotNull
    public GroupingResultsContainer getResultsContainer() {
        return resultsContainer;
    }


    public ChangeSortingAction(@Nullable Boolean descending, @NotNull GroupingResultsContainer resultsContainer) {
        super(
            descending == null ?
                ResultSetMessages.grouping_panel_sorting_action_unsorted :
                (descending ?
                    ResultSetMessages.grouping_panel_sorting_action_descending :
                    ResultSetMessages.grouping_panel_sorting_action_ascending),
            Action.AS_RADIO_BUTTON
        );
        this.descending = descending;
        this.resultsContainer = resultsContainer;
    }

    @Override
    public boolean isChecked() {
        DBPDataSource dataSource = getResultsContainer().getDataContainer().getDataSource();
        if (dataSource == null) {
            return false;
        }
        String defSorting = dataSource.getContainer().getPreferenceStore().getString(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING);
        if (CommonUtils.isEmpty(defSorting)) {
            return descending == null;
        } else if (defSorting.equals(ASC)) {
            return Boolean.FALSE.equals(descending);
        } else {
            return Boolean.TRUE.equals(descending);
        }
    }

    @Override
    public void run() {
        String newValue = descending == null ? NONE : (descending ? DESC : ASC);
        DBPDataSource dataSource = getResultsContainer().getDataContainer().getDataSource();
        if (dataSource == null) {
            return;
        }
        dataSource.getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING, newValue);
        dataSource.getContainer().persistConfiguration();
        try {
            getResultsContainer().rebuildGrouping();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI()
                .showError(ResultSetMessages.grouping_panel_error_title, ResultSetMessages.grouping_panel_error_change_sort_message, e);
        }
    }

}