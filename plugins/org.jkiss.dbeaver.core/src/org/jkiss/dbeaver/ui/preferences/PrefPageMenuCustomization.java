/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.registry.ConfirmationDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PrefPageMenuCustomization extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private class CommandWithStatus {

        private String commandId;
        private String name;
        private boolean enabled;
        
        public CommandWithStatus(String commandId, String name, boolean enabled) {
            this.commandId = commandId;
            this.name = name;
            this.enabled = enabled;
        }

    }


    // TODO: get toolbars from SQL Editor
    ToolBarManager topBarMan = new ToolBarManager();
    Table commandsTable;
    List<CommandWithStatus> commands;
    private Map<ConfirmationDescriptor, Object> changedConfirmations = new HashMap<>();

    @Override
    public void init(IWorkbench workbench) {
        // TODO: get it from toolbar manager - it's only for testing here now
        topBarMan.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), SQLEditorCommands.CMD_EXECUTE_STATEMENT));
        topBarMan.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), SQLEditorCommands.CMD_EXECUTE_STATEMENT_NEW));
        topBarMan.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), SQLEditorCommands.CMD_EXECUTE_SCRIPT));
        topBarMan.add(ActionUtils.makeCommandContribution(UIUtils.getActiveWorkbenchWindow(), SQLEditorCommands.CMD_EXPLAIN_PLAN));
        commands = new ArrayList<>();

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group groupEditors = UIUtils.createControlGroup(composite, "Top SQL Editor toolbar", 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

        TableViewer tableViewer = new TableViewer(
                groupEditors,
                SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

        commandsTable = tableViewer.getTable();
        commandsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        commandsTable.setHeaderVisible(true);
        commandsTable.setLinesVisible(true);

        ViewerColumnController<Object, Object> columnsController = new ViewerColumnController<>(
            "PrefPageMenuCustomizationEditor", //$NON-NLS-1$
            tableViewer
        );

        columnsController.addBooleanColumn(
                "Visible",
                "Indicates whether this command should be added to the toolbar or not",
                SWT.CENTER,
                true,
                true,
                item -> {
                    if (item instanceof CommandWithStatus) {
                        return  ((CommandWithStatus) item).enabled;
                    }
                    return false;
                }, new EditingSupport(tableViewer) {

                    @Override
                    protected CellEditor getCellEditor(Object element) {
                        return new CustomCheckboxCellEditor(tableViewer.getTable(), true);
                    }

                    @Override
                    protected boolean canEdit(Object element) {
                        return true;
                    }

                    @Override
                    protected Object getValue(Object element) {
                        if (element instanceof CommandWithStatus) {
                            return ((CommandWithStatus) element).enabled;
                        }
                        return false;
                    }

                    @Override
                    protected void setValue(Object element, Object value) {
                        if (element instanceof CommandWithStatus) {
                            CommandWithStatus command = (CommandWithStatus) element;
                            command.enabled = CommonUtils.getBoolean(value, true);
                            //changedCommands.put((command).commandId, value);
                        }

                    }
                });

        // TODO: add icon for each column

        columnsController.addColumn(
                "Command",
                "Tip",
                SWT.LEFT,
                true,
                true,
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof CommandWithStatus) {
                            return ((CommandWithStatus) element).name;
                        }
                        return super.getText(element);
                    }

                });

        columnsController.createColumns(false);
        tableViewer.setContentProvider(new ListContentProvider());
        new DefaultViewerToolTipSupport(tableViewer);

        for (IContributionItem item : topBarMan.getItems()) {
            if (item instanceof CommandContributionItem) {
                CommandContributionItem command = (CommandContributionItem) item;
                String commandName;
                try {
                    commandName = command.getCommand().getName();
                } catch (NotDefinedException e) {
                    commandName = command.getCommand().getId();
                }
                this.commands.add(new CommandWithStatus(command.getId(), commandName, true));
            }
        }
        tableViewer.setInput(this.commands);
        tableViewer.refresh();

        UIUtils.asyncExec(() -> UIUtils.packColumns(commandsTable, false));

        return composite;
    }
}
