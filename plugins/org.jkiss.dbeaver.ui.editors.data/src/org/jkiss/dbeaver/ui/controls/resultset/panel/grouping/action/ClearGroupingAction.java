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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;

public class ClearGroupingAction extends GroupingAction {

    public ClearGroupingAction(@NotNull GroupingResultsContainer resultsContainer) {
        super(
            resultsContainer, ResultSetMessages.controls_resultset_grouping_clear,
            DBeaverIcons.getImageDescriptor(UIIcon.CLEAN)
        );
    }

    @Override
    public boolean isEnabled() {
        return !groupingResultsContainer.getGroupAttributes().isEmpty();
    }

    @Override
    public void run() {
        groupingResultsContainer.clearGrouping();
        groupingResultsContainer.getOwnerPresentation().getController().updatePanelActions();
    }
}
