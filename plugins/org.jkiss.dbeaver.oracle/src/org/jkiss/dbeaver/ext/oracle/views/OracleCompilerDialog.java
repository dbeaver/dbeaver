/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleCompileUnit;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * DriverEditDialog
 */
public class OracleCompilerDialog extends TrayDialog
{
    static final Log log = LogFactory.getLog(OracleCompilerDialog.class);

    private static final int COMPILE_ID = 1000;
    private static final int COMPILE_ALL_ID = 1001;

    private java.util.List<OracleCompileUnit> compileUnits;
    private TableViewer unitTable;
    private Table infoTable;

    private final CompileLog compileLog = new CompileLog();
    
    private class CompileLog extends SimpleLog {

        public CompileLog()
        {
            super("Compile log");
        }

        @Override
        protected void log(final int type, final Object message, final Throwable t)
        {
            UIUtils.runInUI(getShell(), new Runnable() {
                public void run()
                {
                    if (infoTable == null || infoTable.isDisposed()) {
                        return;
                    }
                    final TableItem item = new TableItem(infoTable, SWT.NONE);
                    item.setText(CommonUtils.toString(message));
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
                    if (color != -1) {
                        item.setForeground(infoTable.getDisplay().getSystemColor(color));
                    }
                    infoTable.showItem(item);
                    if (t != null) {
                        String prevMessage = null;
                        for (Throwable error = t; error != null; error = error.getCause()) {
                            final String errorMessage = t.getMessage();
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
    }
    
    public OracleCompilerDialog(Shell shell, java.util.List<OracleCompileUnit> compileUnits)
    {
        super(shell);
        this.compileUnits = compileUnits;
        compileLog.setLevel(CompileLog.LOG_LEVEL_ALL);
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Compile object(s)");

        GridData gd;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite unitsGroup = new Composite(composite, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 250;
            gd.heightHint = 200;
            gd.verticalIndent = 0;
            gd.horizontalIndent = 0;
            unitsGroup.setLayoutData(gd);
            unitsGroup.setLayout(new GridLayout(1, false));

            unitTable = new TableViewer(unitsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

            {
                final Table table = unitTable.getTable();
                table.setLayoutData(new GridData(GridData.FILL_BOTH));
                table.setLinesVisible(true);
                table.setHeaderVisible(true);
            }

            final TableViewerColumn pathColumn = UIUtils.createTableViewerColumn(unitTable, SWT.NONE, "Name");
            pathColumn.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DBSObject unit = (DBSObject) cell.getElement();
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
                    if (node != null) {
                        cell.setText(node.getNodeName());
                        cell.setImage(node.getNodeIconDefault());
                    } else {
                        cell.setText(unit.toString());
                    }
                }
            });
            final TableViewerColumn versionColumn = UIUtils.createTableViewerColumn(unitTable, SWT.NONE, "Type");
            versionColumn.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DBSObject unit = (DBSObject) cell.getElement();
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
                    if (node != null) {
                        cell.setText(node.getNodeType());
                    } else {
                        cell.setText("???");
                    }
                }
            });
            unitTable.addSelectionChangedListener(new ISelectionChangedListener() {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    getButton(COMPILE_ID).setEnabled(!selection.isEmpty());

                }
            });
            unitTable.addDoubleClickListener(new IDoubleClickListener() {
                public void doubleClick(DoubleClickEvent event)
                {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    if (!selection.isEmpty()) {
                        OracleCompileUnit unit = (OracleCompileUnit) selection.getFirstElement();
                        NavigatorHandlerObjectOpen.openEntityEditor(unit);
                    }
                }
            });
            unitTable.setContentProvider(new ListContentProvider());
            unitTable.setInput(compileUnits);
            UIUtils.packColumns(unitTable.getTable());
        }

        {
            Composite infoGroup = new Composite(composite, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 400;
            gd.heightHint = 200;
            gd.verticalIndent = 0;
            gd.horizontalIndent = 0;
            infoGroup.setLayoutData(gd);
            infoGroup.setLayout(new GridLayout(1, false));

            infoTable = new Table(infoGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
            infoTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            createContextMenu();
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		// create OK and Cancel buttons by default
        createButton(parent, COMPILE_ID, "Compi&le", false).setEnabled(false);
		createButton(parent, COMPILE_ALL_ID, "Compile &All", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
            IDialogConstants.CLOSE_LABEL, false);
    }

    protected void okPressed()
    {
        super.okPressed();
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        final List<OracleCompileUnit> toCompile;
        if (buttonId == COMPILE_ID) {
            toCompile = ((IStructuredSelection) unitTable.getSelection()).toList();
        } else if (buttonId == COMPILE_ALL_ID) {
            toCompile = compileUnits;
        } else {
            toCompile = null;
        }

        if (!CommonUtils.isEmpty(toCompile)) {
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        performCompilation(monitor, toCompile);
                    }
                });
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(getShell(), "Compile error", null, e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(infoTable);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action("Copy") {
                    public void run()
                    {
                        copySelectionToClipboard();
                    }
                };
                copyAction.setEnabled(infoTable.getSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction selectAllAction = new Action("Select All") {
                    public void run()
                    {
                        infoTable.selectAll();
                    }
                };
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                IAction clearLogAction = new Action("Clear Log") {
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

    private void performCompilation(DBRProgressMonitor monitor, List<OracleCompileUnit> units)
    {
        for (OracleCompileUnit unit : units) {
            final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
            if (node == null) {
                log.warn("Can't find node for object '" + unit + "'");
            }
            if (monitor.isCanceled()) {
                break;
            }
            final String message = "Compile " + node.getNodeType() + " '" + unit.getName() + "' ...";
            compileLog.info(message);

            final IDatabasePersistAction[] compileActions = unit.getCompileActions();
            if (CommonUtils.isEmpty(compileActions)) {
                continue;
            }

            final JDBCExecutionContext context = (JDBCExecutionContext)unit.getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL, message);
            try {
                boolean hasErrors = false;
                for (IDatabasePersistAction action : compileActions) {
                    final String script = action.getScript();
                    compileLog.trace(script);

                    if (monitor.isCanceled()) {
                        break;
                    }
                    try {
                        final DBCStatement dbStat = context.prepareStatement(
                            DBCStatementType.QUERY,
                            script,
                            false, false, false);
                        try {
                            dbStat.executeStatement();
                        } finally {
                            dbStat.close();
                        }
                        action.handleExecute(null);
                    } catch (DBCException e) {
                        log.error("Compile error", e);
                        action.handleExecute(e);
                    }
                    if (action instanceof OracleObjectPersistAction) {
                        final boolean actionFailed = logObjectErrors(context, compileLog, unit, ((OracleObjectPersistAction) action).getObjectType());
                        if (actionFailed) {
                            hasErrors = true;
                        }
                    }
                }
                compileLog.info(hasErrors ? "Compilation errors occurred" : "Successfully compiled");
                compileLog.info("");
            } finally {
                context.close();
            }
        }

    }

    private boolean logObjectErrors(
        JDBCExecutionContext context,
        CompileLog log,
        OracleCompileUnit unit,
        OracleObjectType objectType)
    {
        try {
            final PreparedStatement dbStat = context.prepareStatement(
                DBCStatementType.QUERY,
                "SELECT * FROM SYS.USER_ERRORS where NAME=? AND TYPE=? ORDER BY SEQUENCE",
                false, false, false);
            try {
                dbStat.setString(1, unit.getName());
                dbStat.setString(2, objectType.getTypeName());
                final ResultSet dbResult = dbStat.executeQuery();
                try {
                    boolean hasErrors = false;
                    while (dbResult.next()) {
                        final String attribute = dbResult.getString("ATTRIBUTE");
                        final String text = dbResult.getString("TEXT");
                        hasErrors = true;
                        if ("ERROR".equals(attribute)) {
                            compileLog.error(text);
                        } else {
                            compileLog.warn(text);
                        }
                    }
                    return hasErrors;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        } catch (Exception e) {
            log.error("Can't read user errors", e);
            return true;
        }
    }

}
