/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetDecorator;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator for grouping panel
 */
public class GroupingResultsDecorator implements IResultSetDecorator {

    private GroupingResultsContainer container;

    public GroupingResultsDecorator(GroupingResultsContainer container) {
        this.container = container;
    }

    @Override
    public long getDecoratorFeatures() {
        return FEATURE_NONE;
    }

    @Override
    public String getEmptyDataMessage() {
        if (container.getGroupAttributes().isEmpty()) {
            return ResultSetMessages.results_decorator_no_groupings;
        } else {
            return ResultSetMessages.results_decorator_grouping_failed;
        }
    }

    @Override
    public String getEmptyDataDescription() {
        DBPDataSource dataSource = container.getResultSetController().getDataContainer().getDataSource();
        if (dataSource == null) {
            return ResultSetMessages.results_decorator_no_connected_to_db;
        }
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        if (dialect == null || !dialect.supportsSubqueries()) {
            return ResultSetMessages.results_decorator_grouping_is_not_supported + dataSource.getContainer().getDriver().getFullName() + "'"; //$NON-NLS-2$
        } else {
            if (container.getGroupAttributes().isEmpty()) {
                return ResultSetMessages.results_decorator_drag_and_drop_results_column;
            } else {
                return ResultSetMessages.results_decorator_grouping_attempt_failed;
            }
        }
    }

    @Override
    public void fillContributions(IContributionManager contributionManager) {
        contributionManager.add(new GroupingPanel.EditColumnsAction(container));
        contributionManager.add(new GroupingPanel.DeleteColumnAction(container));
        contributionManager.add(new GroupingPanel.ClearGroupingAction(container));
    }

    @Override
    public void registerDragAndDrop(IResultSetPresentation presentation) {
        // Register drop target to accept columns dropping
        Object oldDropTarget = presentation.getControl().getData(DND.DROP_TARGET_KEY);
        if (oldDropTarget instanceof DropTarget) {
            ((DropTarget) oldDropTarget).dispose();
        }
        DropTarget dropTarget = new DropTarget(presentation.getControl(), DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(LightGrid.GridColumnTransfer.INSTANCE, TextTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event) {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE || event.detail == DND.DROP_COPY) {
                    dropColumns(event);
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event) {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event) {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    if (event.detail == DND.DROP_NONE) {
                        event.detail = DND.DROP_MOVE;
                    }
                }
                event.feedback = DND.FEEDBACK_SELECT;
            }

            private boolean isDropSupported(DropTargetEvent event) {
                return true;
                // TODO: check type
                //ArrayUtils.contains(event.dataTypes, LightGrid.GridColumnTransfer.INSTANCE);
            }

            @SuppressWarnings("unchecked")
            private void dropColumns(DropTargetEvent event) {
                if (!(event.data instanceof List)) {
                    return;
                }
                List<Object> dropElements = (List<Object>) event.data;
                List<String> attributeBindings = new ArrayList<>();
                for (Object element : dropElements) {
                    if (element instanceof DBDAttributeBinding) {
                        DBDAttributeBinding binding = (DBDAttributeBinding) element;
                        if (binding instanceof DBDAttributeBindingMeta && binding.getMetaAttribute() != null) {
                            attributeBindings.add(DBUtils.getQuotedIdentifier(binding.getDataSource(), binding.getMetaAttribute().getLabel()));
                        } else {
                            attributeBindings.add(binding.getFullyQualifiedName(DBPEvaluationContext.DML));
                        }
                    }
                }
                if (!attributeBindings.isEmpty()) {
                    container.addGroupingAttributes(attributeBindings);
                }
                if (event.detail == DND.DROP_COPY) {
                    GroupingConfigDialog dialog = new GroupingConfigDialog(container.getResultSetController().getControl().getShell(), container);
                    if (dialog.open() != IDialogConstants.OK_ID) {
                        container.clearGrouping();
                        return;
                    }
                }
                try {
                    container.rebuildGrouping();
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError(ResultSetMessages.results_decorator_error_grouping_error, ResultSetMessages.results_decorator_error_cant_perform_grouping_query, e);
                }
            }
        });
    }

}
