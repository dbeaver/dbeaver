/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.e4;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerRenameFile;

import java.io.File;

public class DBeaverStackRenderer extends StackRenderer {

    @Override
    protected void populateTabMenu(Menu menu, MPart part) {
        super.populateTabMenu(menu, part);

        IWorkbenchPart workbenchPart = getWorkbenchPart(part);
        if (workbenchPart instanceof IEditorPart) {
            IWorkbenchPage activePage = workbenchPart.getSite().getWorkbenchWindow().getActivePage();
            if (activePage.getActiveEditor() != workbenchPart) {
                activePage.activate(workbenchPart);
            }

            IEditorInput editorInput = ((IEditorPart) workbenchPart).getEditorInput();
            if (editorInput instanceof IDatabaseEditorInput) {
                populateEditorMenu(menu, (IDatabaseEditorInput) editorInput);
            }

            File localFile = EditorUtils.getLocalFileFromInput(editorInput);
            if (localFile != null) {
                populateFileMenu(menu, workbenchPart, EditorUtils.getFileFromInput(editorInput), localFile);
            }
        }
    }

    private void populateFileMenu(@NotNull final Menu menu, @NotNull final IWorkbenchPart workbenchPart, @Nullable final IFile inputFile, @NotNull final File file) {
        new MenuItem(menu, SWT.SEPARATOR);
        if (workbenchPart instanceof SQLEditor) {
            addActionItem(workbenchPart, menu, SQLEditorCommands.CMD_SQL_EDITOR_NEW);
        }
        {
            MenuItem menuItemOpenFolder = new MenuItem(menu, SWT.NONE);
            menuItemOpenFolder.setText(CoreMessages.editor_file_open_in_explorer);
            menuItemOpenFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (file.getParentFile().isDirectory()) {
                        ShellUtils.launchProgram(file.getParentFile().getAbsolutePath());
                    }
                }
            });
        }
        {
            MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
            menuItemOthers.setText(CoreMessages.editor_file_copy_path);
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String filePath = file.getAbsolutePath();
                    UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), filePath);
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        {
            if (workbenchPart instanceof SQLEditor) {
                addActionItem(workbenchPart, menu, SQLEditorCommands.CMD_SQL_DELETE_THIS_SCRIPT);
                addActionItem(workbenchPart, menu, SQLEditorCommands.CMD_SAVE_FILE);
            }

            if (inputFile != null) {
                MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
                String renameText = CoreMessages.editor_file_rename;
                if (workbenchPart instanceof SQLEditor) {
                    renameText += "\t" + ActionUtils.findCommandDescription(SQLEditorCommands.CMD_SQL_RENAME, workbenchPart.getSite(), true); //$NON-NLS-1$
                }
                menuItemOthers.setText(renameText);
                menuItemOthers.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        SQLEditorHandlerRenameFile.renameFile(workbenchPart, inputFile, "file"); //$NON-NLS-1$
                    }
                });
            }
        }

    }

    private void populateEditorMenu(@NotNull Menu menu, @NotNull IDatabaseEditorInput input) {
        final DBSObject object = input.getDatabaseObject();
        final DBNDatabaseNode node = input.getNavigatorNode();

        if (object != null && node != null) {
            final String label = node.getMeta().getNodeTypeLabel(object.getDataSource(), null);

            if (label != null) {
                new MenuItem(menu, SWT.SEPARATOR);

                final MenuItem item = new MenuItem(menu, SWT.NONE);
                item.setText(NLS.bind(CoreMessages.editor_file_copy_object_name, label));
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBWorkbench.getPlatformUI().copyTextToClipboard(DBUtils.getObjectFullName(object, DBPEvaluationContext.UI), false);
                    }
                });
            }
        }
    }

    private static void addActionItem(@NotNull IWorkbenchPart workbenchPart, @NotNull Menu menu, @NotNull String actionId) {
        String actionText = ActionUtils.findCommandName(actionId);
        String shortcut = ActionUtils.findCommandDescription(actionId, workbenchPart.getSite(), true);//$NON-NLS-1$
        if (shortcut != null) {
            actionText += "\t" + shortcut;
        }

        MenuItem menuItemDelete = new MenuItem(menu, SWT.NONE);
        menuItemDelete.setText(actionText);
        menuItemDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ActionUtils.runCommand(actionId, workbenchPart.getSite());
            }
        });
    }

    private IWorkbenchPart getWorkbenchPart(MPart part) {
        if (part != null) {
            Object clientObject = part.getObject();
            if (clientObject instanceof CompatibilityPart) {
                return ((CompatibilityPart) clientObject).getPart();
            }
        }
        return null;
    }

}