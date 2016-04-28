/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.actions.CompileHandler;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ViewerColumnController;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ObjectCompilerLogViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * OracleCompilerDialog
 */
public class OracleCompilerDialog extends BaseDialog
{
    private static final Log log = Log.getLog(OracleCompilerDialog.class);

    private static final int COMPILE_ID = 1000;
    private static final int COMPILE_ALL_ID = 1001;

    private java.util.List<OracleSourceObject> compileUnits;
    private TableViewer unitTable;

    private ObjectCompilerLogViewer compileLog;


    public OracleCompilerDialog(Shell shell, java.util.List<OracleSourceObject> compileUnits)
    {
        super(shell, OracleMessages.views_oracle_compiler_dialog_title, null);
        this.compileUnits = compileUnits;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
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

            ViewerColumnController columnController = new ViewerColumnController("OracleCompilerDialog", unitTable);
            columnController.addColumn(OracleMessages.views_oracle_compiler_dialog_column_name, null, SWT.NONE, true, true, new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DBSObject unit = (DBSObject) cell.getElement();
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
                    if (node != null) {
                        cell.setText(node.getNodeName());
                        cell.setImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
                    } else {
                        cell.setText(unit.toString());
                    }
                }
            });
            columnController.addColumn(OracleMessages.views_oracle_compiler_dialog_column_type, null, SWT.NONE, true, true, new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DBSObject unit = (DBSObject) cell.getElement();
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
                    if (node != null) {
                        cell.setText(node.getNodeType());
                    } else {
                        cell.setText("???"); //$NON-NLS-1$
                    }
                }
            });
            columnController.createColumns();
            unitTable.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    getButton(COMPILE_ID).setEnabled(!selection.isEmpty());

                }
            });
            unitTable.addDoubleClickListener(new IDoubleClickListener() {
                @Override
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

            compileLog = new ObjectCompilerLogViewer(infoGroup, true);
        }

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		// create OK and Cancel buttons by default
        createButton(parent, COMPILE_ID, OracleMessages.views_oracle_compiler_dialog_button_compile, false).setEnabled(false);
		createButton(parent, COMPILE_ALL_ID, OracleMessages.views_oracle_compiler_dialog_button_compile_all, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
            IDialogConstants.CLOSE_LABEL, false);
    }

    @Override
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
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
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
            final String message = NLS.bind(OracleMessages.views_oracle_compiler_dialog_message_compile_unit, unit.getSourceType().name(), unit.getName());
            compileLog.info(message);
            boolean success = false;
            try {
                success = CompileHandler.compileUnit(monitor, compileLog, unit);
            } catch (DBCException e) {
                log.error("Compile error", e);
            }

            compileLog.info(!success ? OracleMessages.views_oracle_compiler_dialog_message_compilation_error : OracleMessages.views_oracle_compiler_dialog_message_compilation_success);
            compileLog.info(""); //$NON-NLS-1$
        }

    }

}
