/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
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

    public ObjectCompilerLogViewer(Composite parent, boolean bordered)
    {
        super();

        infoTable = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | (bordered ? SWT.BORDER : SWT.NONE));
        infoTable.setHeaderVisible(true);
        infoTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Message");
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Line");
        UIUtils.createTableColumn(infoTable, SWT.LEFT, "Pos");

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
        UIUtils.runInUI(null, new Runnable() {
            @Override
            public void run()
            {
                if (!infoTable.isDisposed()) {
                    infoTable.getColumn(0).setWidth(infoTable.getBounds().width - 110);
                    infoTable.getColumn(1).setWidth(50);
                    infoTable.getColumn(2).setWidth(50);
                }
            }
        });
    }

    @Override
    protected void log(final int type, final Object message, final Throwable t)
    {
        super.log(type, message, t);
        UIUtils.runInUI(null, new Runnable() {
            @Override
            public void run()
            {
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
                    error = (DBCCompileError)message;
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
                        item.setText(2, String.valueOf(((DBCCompileError)message).getPosition()));
                    }
                    if (color != -1) {
                        item.setForeground(infoTable.getDisplay().getSystemColor(color));
                    }
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
            }
        });
    }


    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(infoTable);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
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
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        infoTable.setMenu(menu);
    }

    public void copySelectionToClipboard()
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
