/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.oracle.actions.CompileHandler;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * DriverEditDialog
 */
public class OracleCompilerDialog extends TrayDialog
{
    static final Log log = LogFactory.getLog(OracleCompilerDialog.class);

    private static final int COMPILE_ID = 1000;
    private static final int COMPILE_ALL_ID = 1001;

    private java.util.List<OracleSourceObject> compileUnits;
    private TableViewer unitTable;

    private OracleCompilerLogViewer compileLog;


    public OracleCompilerDialog(Shell shell, java.util.List<OracleSourceObject> compileUnits)
    {
        super(shell);
        this.compileUnits = compileUnits;
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
                        OracleSourceObject unit = (OracleSourceObject) selection.getFirstElement();
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

            compileLog = new OracleCompilerLogViewer(infoGroup);
            compileLog.setLevel(OracleCompilerLogViewer.LOG_LEVEL_ALL);
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
        final List<OracleSourceObject> toCompile;
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

    private void performCompilation(DBRProgressMonitor monitor, List<OracleSourceObject> units)
    {
        compileLog.layoutLog();
        for (OracleSourceObject unit : units) {
            if (monitor.isCanceled()) {
                break;
            }
            final String message = "Compile " + unit.getSourceType().name() + " '" + unit.getName() + "' ...";
            compileLog.info(message);
            boolean success = false;
            try {
                success = CompileHandler.compileUnit(monitor, compileLog, unit);
            } catch (DBCException e) {
                log.error("Compile error", e);
            }

            compileLog.info(!success ? "Compilation errors occurred" : "Successfully compiled");
            compileLog.info("");
        }

    }

}
