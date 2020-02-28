/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.compile.DBCSourceHost;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.StringTokenizer;

/**
 * UI Compiler log
 */
public class ObjectCompilerLogViewer extends DBCCompileLogBase {

    private Table infoTable;

    public ObjectCompilerLogViewer(Composite parent, DBCSourceHost sourceHost, boolean bordered)
    {
        super();

        infoTable = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | (bordered ? SWT.BORDER : SWT.NONE));
        infoTable.setHeaderVisible(true);
        infoTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Message");
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Line");
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Pos");

        if (sourceHost != null) {
            infoTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    TableItem[] selection = infoTable.getSelection();
                    if (selection.length > 0) {
                        TableItem item = selection[0];
                        Object data = item.getData();
                        if (data instanceof DBCCompileError) {
                            int line = ((DBCCompileError) data).getLine();
                            int position = ((DBCCompileError) data).getPosition();
                            sourceHost.positionSource(line, position);
                            sourceHost.setCompileInfo(((DBCCompileError) data).getMessage(), true);
                        }
                    }
                }
            });
        }

        createContextMenu();
    }

    @Override
    public void clearLog()
    {
        super.clearLog();
        infoTable.removeAll();
    }

    public void layoutLog()
    {
        UIUtils.asyncExec(() -> {
            if (!infoTable.isDisposed()) {
                infoTable.getColumn(0).setWidth(infoTable.getBounds().width - 110);
                infoTable.getColumn(1).setWidth(50);
                infoTable.getColumn(2).setWidth(50);
            }
        });
    }

    @Override
    protected void log(final int type, final Object message, final Throwable t)
    {
        super.log(type, message, t);
        UIUtils.syncExec(() -> {
            if (infoTable == null || infoTable.isDisposed()) {
                return;
            }
            int color = -1;
            switch (type) {
                case LOG_LEVEL_TRACE:
                    color = SWT.COLOR_DARK_BLUE;
                    break;
                case LOG_LEVEL_DEBUG:
                case LOG_LEVEL_INFO:
                    break;
                case LOG_LEVEL_WARN:
                    color = SWT.COLOR_DARK_YELLOW;
                    break;
                case LOG_LEVEL_ERROR:
                case LOG_LEVEL_FATAL:
                    color = SWT.COLOR_DARK_RED;
                    break;
                default:
                    break;
            }
            String messageStr;
            DBCCompileError error = null;
            if (message instanceof DBCCompileError) {
                error = (DBCCompileError) message;
                messageStr = error.getMessage();
            } else {
                messageStr = CommonUtils.toString(message);
            }
            StringTokenizer st = new StringTokenizer(messageStr, "\n"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                final TableItem item = new TableItem(infoTable, SWT.NONE);
                item.setText(0, st.nextToken());
                if (error != null && error.getLine() > 0) {
                    item.setText(1, String.valueOf(((DBCCompileError) message).getLine()));
                    item.setText(2, String.valueOf(((DBCCompileError) message).getPosition()));
                }
                if (color != -1) {
                    item.setForeground(infoTable.getDisplay().getSystemColor(color));
                }
                item.setData(message);
                infoTable.showItem(item);
            }
            if (t != null) {
                String prevMessage = null;
                for (Throwable ex = t; error != null; ex = ex.getCause()) {
                    final String errorMessage = ex.getMessage();
                    if (errorMessage == null || errorMessage.equals(prevMessage)) {
                        continue;
                    }
                    prevMessage = errorMessage;
                    TableItem stackItem = new TableItem(infoTable, SWT.NONE);
                    stackItem.setText(errorMessage);
                    stackItem.setForeground(infoTable.getDisplay().getSystemColor(SWT.COLOR_RED));
                    infoTable.showItem(stackItem);
                }
            }
        });
    }


    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(infoTable);
        menuMgr.addMenuListener(manager -> {
            IAction copyAction = new Action(WorkbenchMessages.Workbench_copy) {
                @Override
                public void run()
                {
                    copySelectionToClipboard();
                }
            };
            copyAction.setEnabled(infoTable.getSelectionCount() > 0);
            copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

            IAction selectAllAction = new Action(WorkbenchMessages.Workbench_selectAll) {
                @Override
                public void run()
                {
                    infoTable.selectAll();
                }
            };
            selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

            IAction clearLogAction = new Action(WorkbenchMessages.Workbench_revert) {
                @Override
                public void run()
                {
                    infoTable.removeAll();
                }
            };

            manager.add(copyAction);
            manager.add(selectAllAction);
            manager.add(clearLogAction);
            //manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        });
        menuMgr.setRemoveAllWhenShown(true);
        infoTable.setMenu(menu);
        infoTable.addDisposeListener(e -> menuMgr.dispose());
    }

    private void copySelectionToClipboard()
    {
        final TableItem[] selection = infoTable.getSelection();
        if (ArrayUtils.isEmpty(selection)) {
            return;
        }
        StringBuilder tdt = new StringBuilder();
        for (TableItem item : selection) {
            tdt.append(item.getText())
                .append(GeneralUtils.getDefaultLineSeparator());
        }
        UIUtils.setClipboardContents(
            infoTable.getDisplay(),
            TextTransfer.getInstance(),
            tdt.toString());
    }

}
