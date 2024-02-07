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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.commands.SQLCommandSet;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.AdvancedTextCellEditor;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EditTaskVariablesDialog
 */
public class EditTaskVariablesDialog extends StatusDialog {

    private static final String DIALOG_ID = "DBeaver.SQLQueryParameterBindDialog";//$NON-NLS-1$

    private final List<TaskVariableList> variables;
    private TreeViewer viewer;

    public EditTaskVariablesDialog(@NotNull Shell shell, @NotNull Map<DBTTask, Map<String, Object>> variables) {
        super(shell);
        setTitle(TaskUIMessages.edit_task_variabl_dialog_title_task_variables);

        this.variables = variables.entrySet().stream()
            .map(entry -> {
                final var key = entry.getKey();
                final var vars = entry.getValue().entrySet().stream()
                    .map(variable -> new TaskVariable(variable.getKey(), CommonUtils.toString(variable.getValue())))
                    .collect(Collectors.toCollection(ArrayList::new));
                return new TaskVariableList(key, vars);
            })
            .toList();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite composite = (Composite) super.createDialogArea(parent);

        final Composite paramsComposite = UIUtils.createComposite(composite, 1);
        paramsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;

        viewer = new TreeViewer(paramsComposite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        viewer.getTree().setLayoutData(gd);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(!UIStyles.isDarkTheme());
        viewer.setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object parent) {
                if (parent instanceof TaskVariableList list) {
                    return list.variables().toArray();
                }
                return new Object[0];
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof TaskVariableList;
            }
        });

        final TreeViewerColumn taskColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        taskColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof TaskVariableList list) {
                    return list.task.getName();
                }
                return null;
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof TaskVariableList list && list.task.getType().getIcon() != null) {
                    return DBeaverIcons.getImage(list.task.getType().getIcon());
                }
                return null;
            }
        });
        taskColumn.getColumn().setText(TaskUIMessages.edit_task_variabl_dialog_column_task);

        final TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof TaskVariable variable) {
                    return variable.name;
                }
                return null;
            }
        });
        nameColumn.setEditingSupport(new EditingSupport(viewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor(viewer.getTree());
            }

            @Override
            protected boolean canEdit(Object element) {
                return element instanceof TaskVariable;
            }

            @Override
            protected Object getValue(Object element) {
                final String value = ((TaskVariable) element).name;
                if (value.chars().anyMatch(Character::isLowerCase)) {
                    return BasicSQLDialect.INSTANCE.getQuotedIdentifier(value, true, false);
                } else {
                    return value;
                }
            }

            @Override
            protected void setValue(Object element, Object value) {
                ((TaskVariable) element).name = SQLCommandSet.prepareVarName(BasicSQLDialect.INSTANCE, (String) value);
                viewer.update(element, null);
            }
        });
        nameColumn.getColumn().setText(TaskUIMessages.edit_task_variabl_dialog_column_variable);

        final TreeViewerColumn valueColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof TaskVariable variable) {
                    return variable.value;
                }
                return null;
            }
        });
        valueColumn.setEditingSupport(new EditingSupport(viewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new AdvancedTextCellEditor(viewer.getTree());
            }

            @Override
            protected boolean canEdit(Object element) {
                return element instanceof TaskVariable;
            }

            @Override
            protected Object getValue(Object element) {
                return ((TaskVariable) element).value;
            }

            @Override
            protected void setValue(Object element, Object value) {
                ((TaskVariable) element).value = (String) value;
                viewer.update(element, null);
            }
        });
        valueColumn.getColumn().setText(TaskUIMessages.edit_task_variabl_dialog_column_value);

        final ToolBar toolbar = new ToolBar(composite, SWT.FLAT | SWT.HORIZONTAL);
        final ToolItem newButton = UIUtils.createToolItem(toolbar, null, UIIcon.ROW_ADD, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final TreePath path = viewer.getStructuredSelection().getPaths()[0];
                final TaskVariableList list = (TaskVariableList) path.getFirstSegment();

                final TaskVariable variable = new TaskVariable("var", "value");
                list.variables.add(variable);
                viewer.refresh(list);
                viewer.setSelection(new StructuredSelection(variable), true);
                viewer.editElement(variable, 1);
            }
        });
        final ToolItem deleteButton = UIUtils.createToolItem(toolbar, null, UIIcon.ROW_DELETE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final TreePath path = viewer.getStructuredSelection().getPaths()[0];
                final TaskVariableList list = (TaskVariableList) path.getFirstSegment();
                final TaskVariable variable = (TaskVariable) path.getLastSegment();
                final List<TaskVariable> variables = list.variables;

                final int index = variables.indexOf(variable);
                variables.remove(index);
                viewer.refresh(list);

                if (!variables.isEmpty()) {
                    viewer.setSelection(new StructuredSelection(variables.get(CommonUtils.clamp(index, 0, variables.size() - 1))));
                }
            }
        });

        newButton.setEnabled(false);
        deleteButton.setEnabled(false);

        viewer.addSelectionChangedListener(event -> {
            final Object element = viewer.getStructuredSelection().getFirstElement();
            newButton.setEnabled(element instanceof TaskVariableList || element instanceof TaskVariable);
            deleteButton.setEnabled(element instanceof TaskVariable);
        });

        UIUtils.asyncExec(() -> {
            viewer.setInput(variables);
            viewer.expandAll(true);
            UIUtils.packColumns(viewer.getTree(), true, new float[]{0.1f, 0.2f, 0.7f});
        });

        return composite;
    }

    @NotNull
    public Map<String, Object> getVariables(@NotNull DBTTask task) {
        return variables.stream()
            .filter(l -> l.task == task)
            .flatMap(l -> l.variables.stream())
            .collect(Collectors.toMap(v -> v.name, v -> v.value));
    }

    private record TaskVariableList(@NotNull DBTTask task, @NotNull List<TaskVariable> variables) {
    }

    private static class TaskVariable {
        private String name;
        private String value;

        private TaskVariable(@NotNull String name, @NotNull String value) {
            this.name = name;
            this.value = value;
        }
    }
}
