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
package org.jkiss.dbeaver.ui.editors.sql.macros;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage SQL macros dialog.
 */
public class SQLMacrosDialog extends BaseDialog {

    private TableViewer macrosViewer;
    private List<SQLMacro> macros;

    public SQLMacrosDialog(@NotNull Shell parentShell) {
        super(parentShell, SQLEditorMessages.dialog_macros_manage_title, null);
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);

        macros = new ArrayList<>(SQLMacrosRegistry.getInstance().getMacros());

        createMacrosTable(composite);
        createButtonsPanel(composite);

        return composite;
    }

    private void createMacrosTable(Composite composite) {
        macrosViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 700;
        gd.heightHint = 300;
        macrosViewer.getTable().setLayoutData(gd);
        macrosViewer.getTable().setLinesVisible(true);
        macrosViewer.getTable().setHeaderVisible(true);

        createColumn(SQLEditorMessages.dialog_macros_manage_column_name, 200);
        createColumn(SQLEditorMessages.dialog_macros_manage_column_shortcut, 100);
        createColumn(SQLEditorMessages.dialog_macros_manage_column_query, 400);

        macrosViewer.setContentProvider(new ContentProvider());
        macrosViewer.setLabelProvider(new MacroLabelProvider());
        macrosViewer.setInput(""); //$NON-NLS-1$

        macrosViewer.getTable().addListener(SWT.MouseDoubleClick, event -> editCurrentMacro());
    }

    private void createColumn(String title, int width) {
        TableViewerColumn column = new TableViewerColumn(macrosViewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
    }

    private void createButtonsPanel(Composite composite) {
        Composite buttonsPanel = UIUtils.createPlaceholder(composite, 3);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.END;
        buttonsPanel.setLayoutData(gd);

        createButton(buttonsPanel, SQLEditorMessages.dialog_macros_manage_button_new, this::createNewMacro);
        createButton(buttonsPanel, SQLEditorMessages.dialog_macros_manage_button_edit, this::editCurrentMacro);
        createButton(buttonsPanel, SQLEditorMessages.dialog_macros_manage_button_delete, this::deleteSelectedMacros);
    }

    private Button createButton(Composite parent, String label, Runnable action) {
        Button button = UIUtils.createPushButton(parent, label, null);
        button.addListener(SWT.Selection, event -> action.run());
        return button;
    }

    private void createNewMacro() {
        SQLMacrosEditDialog dialog = new SQLMacrosEditDialog(getShell(), null);
        if (dialog.open() == OK) {
            SQLMacro macro = dialog.getMacro();
            macros.add(macro);
            refreshTable();
            macrosViewer.setSelection(new StructuredSelection(macro));
        }
    }

    private void editCurrentMacro() {
        SQLMacro current = getSelectedMacro();
        if (current == null) {
            return;
        }
        SQLMacrosEditDialog dialog = new SQLMacrosEditDialog(getShell(), current);
        if (dialog.open() == OK) {
            SQLMacro macro = dialog.getMacro();
            macros.removeIf(existing -> existing == current);
            macros.add(macro);
            refreshTable();
            macrosViewer.setSelection(new StructuredSelection(macro));
        }
    }

    private void deleteSelectedMacros() {
        List<SQLMacro> selected = getSelectedMacros();
        if (selected.isEmpty()) {
            return;
        }
        if (!MessageDialog.openQuestion(getShell(),
                SQLEditorMessages.dialog_macros_manage_delete_confirm_title,
                SQLEditorMessages.dialog_macros_manage_delete_confirm_message)) {
            return;
        }
        macros.removeAll(selected);
        refreshTable();
    }

    @Nullable
    private SQLMacro getSelectedMacro() {
        List<SQLMacro> selected = getSelectedMacros();
        return selected.isEmpty() ? null : selected.get(0);
    }

    @NotNull
    private List<SQLMacro> getSelectedMacros() {
        Table table = macrosViewer.getTable();
        List<SQLMacro> result = new ArrayList<>();
        for (int i : table.getSelectionIndices()) {
            if (i < macros.size()) {
                result.add(macros.get(i));
            }
        }
        return result;
    }

    private void refreshTable() {
        macrosViewer.refresh();
    }

    @Override
    protected void okPressed() {
        // Apply changes made in this dialog: remove deleted macros, then upsert added/edited ones
        for (SQLMacro macro : SQLMacrosRegistry.getInstance().getMacros()) {
            if (macros.stream().noneMatch(existing -> existing.getId().equals(macro.getId()))) {
                SQLMacrosRegistry.getInstance().deleteMacro(macro);
            }
        }
        for (SQLMacro macro : macros) {
            SQLMacrosRegistry.getInstance().updateMacro(macro);
        }
        super.okPressed();
    }

    private final class MacroLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            SQLMacro macro = (SQLMacro) element;
            return switch (columnIndex) {
                case 0 -> macro.getName();
                case 1 -> macro.getShortcutLabel();
                case 2 -> macro.getQuery();
                default -> ""; //$NON-NLS-1$
            };
        }
    }

    private class ContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            return macros.toArray();
        }
    }

}