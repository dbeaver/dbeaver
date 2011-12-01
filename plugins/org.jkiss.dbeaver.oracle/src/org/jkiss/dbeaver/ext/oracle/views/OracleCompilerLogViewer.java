package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleCompileError;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleCompileLogBase;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.StringTokenizer;

/**
 * UI Compiler log
 */
public class OracleCompilerLogViewer extends OracleCompileLogBase {

    private Table infoTable;

    public OracleCompilerLogViewer(Composite parent)
    {
        super();

        infoTable = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        infoTable.setHeaderVisible(true);
        infoTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumn messageColumn = UIUtils.createTableColumn(infoTable, SWT.LEFT, OracleMessages.views_oracle_compiler_log_viewer_column_message);
        TableColumn lineColumn = UIUtils.createTableColumn(infoTable, SWT.LEFT, OracleMessages.views_oracle_compiler_log_viewer_column_line);
        TableColumn posColumn = UIUtils.createTableColumn(infoTable, SWT.LEFT, OracleMessages.views_oracle_compiler_log_viewer_column_pos);

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
                OracleCompileError error = null;
                if (message instanceof OracleCompileError) {
                    error = (OracleCompileError)message;
                    messageStr = error.getMessage();
                } else {
                    messageStr = CommonUtils.toString(message);
                }
                StringTokenizer st = new StringTokenizer(messageStr, "\n"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    final TableItem item = new TableItem(infoTable, SWT.NONE);
                    item.setText(0, st.nextToken());
                    if (error != null && error.getLine() > 0) {
                        item.setText(1, String.valueOf(((OracleCompileError) message).getLine()));
                        item.setText(2, String.valueOf(((OracleCompileError)message).getPosition()));
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
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action(OracleMessages.views_oracle_compiler_log_viewer_action_copy) {
                    public void run()
                    {
                        copySelectionToClipboard();
                    }
                };
                copyAction.setEnabled(infoTable.getSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction selectAllAction = new Action(OracleMessages.views_oracle_compiler_log_viewer_action_select_all) {
                    public void run()
                    {
                        infoTable.selectAll();
                    }
                };
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                IAction clearLogAction = new Action(OracleMessages.views_oracle_compiler_log_viewer_action_clear_log) {
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
        if (CommonUtils.isEmpty(selection)) {
            return;
        }
        StringBuilder tdt = new StringBuilder();
        for (TableItem item : selection) {
            tdt.append(item.getText())
                .append(ContentUtils.getDefaultLineSeparator());
        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        Clipboard clipboard = new Clipboard(infoTable.getDisplay());
        clipboard.setContents(
            new Object[]{tdt.toString()},
            new Transfer[]{textTransfer});
    }

}
