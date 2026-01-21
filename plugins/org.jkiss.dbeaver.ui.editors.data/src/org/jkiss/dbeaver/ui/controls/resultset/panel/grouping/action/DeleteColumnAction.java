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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;
import org.jkiss.utils.ArrayUtils;

import java.util.List;

public class DeleteColumnAction extends GroupingAction {

    public DeleteColumnAction(@NotNull GroupingResultsContainer resultsContainer) {
        super(resultsContainer, ResultSetMessages.controls_resultset_grouping_remove_column, DBeaverIcons.getImageDescriptor(UIIcon.CLOSE));
    }

    @Override
    public boolean isEnabled() {
        return !groupingResultsContainer.getResultSetController().getSelection().isEmpty();
    }

    @Override
    public void run() {
        IResultSetController resultSetController = groupingResultsContainer.getResultSetController();
        DBDAttributeBinding currentBinding = resultSetController.getActivePresentation().getCurrentAttribute();
        if (currentBinding != null) {
            int attrBindingIndex = ArrayUtils.indexOf(resultSetController.getModel().getAttributes(), currentBinding);
            if (attrBindingIndex >= 0 && currentBinding.getDataContainer() instanceof GroupingDataContainer dataContainer) {
                SQLGroupingAttribute[] currAttrs = dataContainer.getGroupingAttributes();
                boolean removed;
                if (currAttrs != null && attrBindingIndex < currAttrs.length) {
                    removed = groupingResultsContainer.removeGroupingAttribute(List.of(currAttrs[attrBindingIndex]));
                } else {
                    removed = groupingResultsContainer.removeGroupingFunction(
                        List.of(currentBinding.getFullyQualifiedName(DBPEvaluationContext.UI))
                    );
                }
                if (removed) {
                    try {
                        groupingResultsContainer.rebuildGrouping();
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError(
                            ResultSetMessages.grouping_panel_error_title,
                            ResultSetMessages.grouping_panel_error_change_grouping_query_message, e
                        );
                    }
                }
            }
        }
    }
}