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
import org.eclipse.core.resources.IFolder;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
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
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerRenameFile;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.io.File;


public class DBeaverStackRenderer extends StackRenderer {

    private static final Log log = Log.getLog(DBeaverStackRenderer.class);

    @Override
    public void showAvailableItems(MElementContainer<?> stack, CTabFolder folder, boolean forceCenter) {
        final IEclipseContext ctx = getContext(stack);
        final DBeaverPartList list = new DBeaverPartList(
            folder.getShell(),
            SWT.ON_TOP,
            SWT.V_SCROLL | SWT.H_SCROLL,
            ctx.get(EPartService.class),
            stack,
            this,
            true
        );

        list.setInput();

        final Point size = list.computeSizeHint();
        final Point location;

        if (forceCenter) {
            // placed to the center
            final Rectangle area = folder.getClientArea();
            location = folder.toDisplay(area.x, area.y);
            location.x = Math.max(0, location.x + (area.width - size.x) / 2);
            location.y = Math.max(0, location.y + (area.height - size.y) / 3);
        } else {
            // placed at chevron location
            location = folder.toDisplay(getChevronLocation(folder));
            final Rectangle area = folder.getMonitor().getClientArea();
            if (location.x + size.x > area.x + area.width) {
                location.x = area.x + area.width - size.x;
            }
            if (location.y + size.y > area.y + area.height) {
                location.y = area.y + area.height - size.y;
            }
        }

        list.setSize(size.x, size.y);
        list.setLocation(location);
        list.setVisible(true);
        list.setFocus();
        list.getShell().addListener(SWT.Deactivate, e -> UIUtils.asyncExec(() -> {
            if (!list.hasFocus()) {
                list.dispose();
            }
        }));
    }

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

            IFile localFile = EditorUtils.getFileFromInput(editorInput);
            if (localFile != null) {
                populateFileMenu(menu, workbenchPart, localFile);
            }

            if (localFile == null && workbenchPart instanceof SQLEditor) {
                new MenuItem(menu, SWT.SEPARATOR);
                addActionItem(workbenchPart, menu, IWorkbenchCommandConstants.FILE_SAVE_AS);
            }
            
            if (workbenchPart instanceof SQLEditor) {
                new MenuItem(menu, SWT.SEPARATOR);
                MenuItem menuItemDisableSQLSyntaxParser = new MenuItem(menu, SWT.CHECK);
                menuItemDisableSQLSyntaxParser.setText(SQLEditorMessages.sql_editor_prefs_disable_services_text);
                menuItemDisableSQLSyntaxParser.setToolTipText(SQLEditorMessages.sql_editor_prefs_disable_services_tip);
                
                menuItemDisableSQLSyntaxParser.setSelection(!SQLEditorUtils.isSQLSyntaxParserApplied(editorInput));
                menuItemDisableSQLSyntaxParser.setEnabled(ActionUtils.isCommandEnabled(SQLEditorCommands.CMD_DISABLE_SQL_SYNTAX_PARSER, workbenchPart.getSite()));
                
                menuItemDisableSQLSyntaxParser.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        SQLEditorUtils.setSQLSyntaxParserEnabled(editorInput, !SQLEditorUtils.isSQLSyntaxParserEnabled(editorInput));
                        menuItemDisableSQLSyntaxParser.setSelection(!SQLEditorUtils.isSQLSyntaxParserApplied(editorInput));
                    }
                });
            }
        }
    }

    private void populateFileMenu(@NotNull final Menu menu, @NotNull final IWorkbenchPart workbenchPart, @NotNull final IFile inputFile) {
        new MenuItem(menu, SWT.SEPARATOR);
        if (workbenchPart instanceof SQLEditor) {
            addActionItem(workbenchPart, menu, SQLEditorCommands.CMD_SQL_EDITOR_NEW);
        }
        if (inputFile.getParent() instanceof IFolder && inputFile.getParent().getLocation() != null) {
            MenuItem menuItemOpenFolder = new MenuItem(menu, SWT.NONE);
            menuItemOpenFolder.setText(CoreMessages.editor_file_open_in_explorer);
            menuItemOpenFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(inputFile.getParent().getLocation().toFile().getAbsolutePath());
                }
            });
        }
        if (inputFile.getLocation() != null) {
            MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
            menuItemOthers.setText(CoreMessages.editor_file_copy_path);
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String filePath = inputFile.getLocation().toFile().getAbsolutePath();
                    UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), filePath);
                }
            });
        }

        new MenuItem(menu, SWT.SEPARATOR);

        if (!inputFile.isReadOnly()) {
            if (workbenchPart instanceof SQLEditor) {
                addActionItem(workbenchPart, menu, SQLEditorCommands.CMD_SQL_DELETE_THIS_SCRIPT);
            }

            {
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

    @NotNull
    private static Point getChevronLocation(@NotNull CTabFolder folder) {
        CTabItem item = null;

        for (int i = 0; i < folder.getItemCount(); i++) {
            final CTabItem tmpItem = folder.getItem(i);
            if (tmpItem.isShowing()) {
                item = tmpItem;
            }
        }

        if (item != null) {
            final Rectangle bounds = item.getBounds();
            return new Point(bounds.x + bounds.width, bounds.y + bounds.height);
        } else {
            return new Point(0, 0);
        }
    }
}