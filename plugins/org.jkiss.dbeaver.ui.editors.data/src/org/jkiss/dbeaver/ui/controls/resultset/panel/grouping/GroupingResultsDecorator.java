/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDecoratorBase;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator for grouping panel
 */
public class GroupingResultsDecorator extends ResultSetDecoratorBase {

    private GroupingResultsContainer container;

    public GroupingResultsDecorator(GroupingResultsContainer container) {
        this.container = container;
    }

    @Override
    public long getDecoratorFeatures() {
        return FEATURE_PRESENTATIONS;
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
        /*if (dialect == null) {
            return NLS.bind(ResultSetMessages.results_decorator_grouping_is_not_supported, dataSource.getContainer().getDriver().getFullName());
        } else */{
            if (container.getGroupAttributes().isEmpty()) {
                return ResultSetMessages.results_decorator_drag_and_drop_results_column;
            } else {
                return ResultSetMessages.results_decorator_grouping_attempt_failed;
            }
        }
    }

    @Override
    public void fillContributions(@NotNull IContributionManager contributionManager) {
        contributionManager.add(new GroupingPanel.EditColumnsAction(container));
        contributionManager.add(new GroupingPanel.DeleteColumnAction(container));
        contributionManager.add(new GroupingPanel.ClearGroupingAction(container));
    }

    @Override
    public void registerDragAndDrop(@NotNull IResultSetPresentation presentation) {
        Control presentationControl = presentation.getControl();
        final DropTargetListener[] gridDropListeners;
        // Register drop target to accept columns dropping
        Object oldDropTarget = presentationControl.getData(DND.DROP_TARGET_KEY);
        if (oldDropTarget instanceof DropTarget) {
            gridDropListeners = ((DropTarget) oldDropTarget).getDropListeners();
            ((DropTarget) oldDropTarget).dispose();
        } else {
            gridDropListeners = null;
        }
        DropTarget dropTarget = new DropTarget(presentationControl, DND.DROP_MOVE | DND.DROP_COPY);
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
                List<String> newBindings = new ArrayList<>();
                List<DBDAttributeBinding> movedBindings = new ArrayList<>();
                for (Object element : dropElements) {
                    if (element instanceof DBDAttributeBinding) {
                        DBDAttributeBinding binding = (DBDAttributeBinding) element;
                        String attrName = getAttributeBindingName(binding);

                        if (ArrayUtils.contains(container.getResultSetController().getModel().getAttributes(), binding)) {
                            // Check for group function - can't move function columns
                            if (container.getGroupAttributes().contains(attrName)) {
                                // It is column move, not new binding
                                movedBindings.add(binding);
                            }
                        } else {
                            newBindings.add(attrName);
                        }
                    }
                }
                if (movedBindings.isEmpty() && newBindings.isEmpty()) {
                    return;
                }
                if (!movedBindings.isEmpty()) {
                    if (gridDropListeners != null) {
                        // Do visual reordering if needed
//                        dropElements.clear();
//                        dropElements.addAll(movedBindings);
//                        for (DropTargetListener listener : gridDropListeners) {
//                            listener.drop(event);
//                        }
                    }

                    // Reorder columns
                    List<String> curAttributes = new ArrayList<>(container.getGroupAttributes());
                    if (!(presentation.getControl() instanceof Spreadsheet)) {
                        return;
                    }

                    int overColumnIndex = ((Spreadsheet)presentationControl).getColumnIndex(event.x, event.y);
                    if (overColumnIndex < 0) {
                        return;
                    }
                    if (overColumnIndex >= curAttributes.size()) {
                        overColumnIndex = curAttributes.size() - 1;
                    }

                    for (DBDAttributeBinding mb : movedBindings) {
                        String attrName = getAttributeBindingName(mb);
                        curAttributes.remove(attrName);
                        curAttributes.add(overColumnIndex, attrName);
                    }
                    container.clearGroupingAttributes();
                    container.addGroupingAttributes(curAttributes);
                }

                if (!newBindings.isEmpty()) {
                    container.addGroupingAttributes(newBindings);
                }
                UIUtils.asyncExec(() -> {
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
                });
            }
        });
    }

    private static String getAttributeBindingName(DBDAttributeBinding binding) {
        if (binding instanceof DBDAttributeBindingMeta && binding.getMetaAttribute() != null) {
            return DBUtils.getQuotedIdentifier(binding.getDataSource(), binding.getMetaAttribute().getLabel());
        } else {
            return binding.getFullyQualifiedName(DBPEvaluationContext.DML);
        }
    }

}
