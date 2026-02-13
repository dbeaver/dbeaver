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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetListenerAdapter;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;
import org.jkiss.utils.ArrayUtils;

import java.util.List;
import java.util.function.Supplier;

public class DeleteColumnAction extends GroupingAction {

    private final PercentFromTotalAction percentFromTotalAction;

    @NotNull
    private RemoveColumnStrategy removeColumnStrategy;

    public DeleteColumnAction(@NotNull GroupingResultsContainer resultsContainer) {
        super(resultsContainer, ResultSetMessages.controls_resultset_grouping_remove_column, DBeaverIcons.getImageDescriptor(UIIcon.CLOSE));
        this.percentFromTotalAction = new PercentFromTotalAction(resultsContainer);
        groupingResultsContainer.getResultSetController().addListener(new ResultSetListenerAdapter() {
            @Override
            public void handleResultSetSelectionChange(SelectionChangedEvent event) {
                defineStrategyAndCheckEnable();
            }
        });
        defineStrategyAndCheckEnable();
    }

    private void defineStrategyAndCheckEnable() {
        removeColumnStrategy = defineRemoveStrategy();
        setEnabled(isEnabled());
    }

    @Override
    public boolean isEnabled() {
        return removeColumnStrategy.isColumnPossibleToRemove();
    }

    @Override
    public void run() {
        boolean removed = removeColumnStrategy.deleteColumn();
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


    @NotNull
    private RemoveColumnStrategy defineRemoveStrategy() {
        IResultSetController resultSetController = groupingResultsContainer.getResultSetController();
        DBDAttributeBinding currentBinding = resultSetController.getActivePresentation().getCurrentAttribute();
        if (currentBinding != null) {
            int attrBindingIndex = ArrayUtils.indexOf(resultSetController.getModel().getAttributes(), currentBinding);
            if (attrBindingIndex >= 0 && currentBinding.getDataContainer() instanceof GroupingDataContainer dataContainer) {
                SQLGroupingAttribute[] currAttrs = dataContainer.getGroupingAttributes();
                Supplier<Boolean> removeFunction;
                if (currAttrs != null && attrBindingIndex < currAttrs.length) {
                    removeFunction = () -> groupingResultsContainer.removeGroupingAttribute(List.of(currAttrs[attrBindingIndex]));
                    return new RemoveColumnStrategy(InstanceType.ATTRIBUTE, removeFunction);
                } else if (attrBindingIndex == groupingResultsContainer.getPercentFunctionOrderInStatement()) {
                    removeFunction = () -> {
                        percentFromTotalAction.setChecked(false);
                        percentFromTotalAction.run();
                        return true;
                    };
                    return new RemoveColumnStrategy(InstanceType.PERCENT_GROUPING_FUNCTION, removeFunction);
                } else {
                    removeFunction = () -> groupingResultsContainer.removeGroupingFunction(
                        List.of(currentBinding.getFullyQualifiedName(DBPEvaluationContext.UI))
                    );
                    return new RemoveColumnStrategy(InstanceType.GROUPING_FUNCTION, removeFunction);
                }
            }
        }
        return getEmptyStrategy();
    }

    @NotNull
    private RemoveColumnStrategy getEmptyStrategy() {
        return new RemoveColumnStrategy(InstanceType.NONE, () -> false);
    }

    private class RemoveColumnStrategy {

        private final InstanceType instanceType;

        private final Supplier<Boolean> removingColumnFunction;

        public RemoveColumnStrategy(@NotNull InstanceType instanceType, @NotNull Supplier<Boolean> removingColumnFunction) {
            this.instanceType = instanceType;
            this.removingColumnFunction = removingColumnFunction;
        }

        public boolean isColumnPossibleToRemove() {
            return isColumnSelectionNotEmpty() && canStrategyTypeBeUsed();
        }

        public boolean deleteColumn() {
            return removingColumnFunction.get();
        }

        private boolean isColumnSelectionNotEmpty() {
            return !groupingResultsContainer.getResultSetController().getSelection().isEmpty();
        }

        private boolean canStrategyTypeBeUsed() {
            return switch (instanceType) {
                case ATTRIBUTE -> groupingResultsContainer.getGroupAttributes().size() > 1;
                case GROUPING_FUNCTION -> groupingResultsContainer.getGroupFunctions().size() > 1;
                case PERCENT_GROUPING_FUNCTION -> groupingResultsContainer.getPercentFunctionOrderInStatement() >= 0;
                case NONE -> false;
            };
        }
    }

    private enum InstanceType {
        ATTRIBUTE,
        GROUPING_FUNCTION,
        PERCENT_GROUPING_FUNCTION,
        NONE;
    }
}