/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import com.sun.javaws.OperaSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleCompileUnit;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverPathDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;

import java.util.*;
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
    private TableViewer infoTable;

    private java.util.List<DriverPathDescriptor> homeDirs = new ArrayList<DriverPathDescriptor>();

    private String curFolder = null;
    private ListViewer logList;

    public OracleCompilerDialog(Shell shell, java.util.List<OracleCompileUnit> compileUnits)
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

            unitTable = new TableViewer(unitsGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

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
                    cell.setText(node.getNodeName());
                    cell.setImage(node.getNodeIconDefault());
                }
            });
            final TableViewerColumn versionColumn = UIUtils.createTableViewerColumn(unitTable, SWT.NONE, "Type");
            versionColumn.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DBSObject unit = (DBSObject) cell.getElement();
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(unit);
                    cell.setText(node.getNodeType());
                }
            });
            unitTable.addSelectionChangedListener(new ISelectionChangedListener() {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    selectUnit(selection.isEmpty() ? null : (OracleCompileUnit)selection.getFirstElement());
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

            infoTable = new TableViewer(infoGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

            {
                final Table table = infoTable.getTable();
                table.setLayoutData(new GridData(GridData.FILL_BOTH));
                table.setLinesVisible(true);
            }
            final TableViewerColumn titleColumn = UIUtils.createTableViewerColumn(infoTable, SWT.NONE, "Title");
            titleColumn.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    IDatabasePersistAction action = (IDatabasePersistAction) cell.getElement();
                    cell.setText(action.getTitle());
                }
            });
            final TableViewerColumn sqlColumn = UIUtils.createTableViewerColumn(infoTable, SWT.NONE, "SQL");
            sqlColumn.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    IDatabasePersistAction action = (IDatabasePersistAction) cell.getElement();
                    cell.setText(action.getScript());
                }
            });

            infoTable.setContentProvider(new ListContentProvider());
            UIUtils.packColumns(infoTable.getTable());

            logList = new ListViewer(infoGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            logList.setLabelProvider(new ColumnLabelProvider() {

            });
            logList.getList().setLayoutData(new GridData(GridData.FILL_BOTH));
            logList.setContentProvider(new ListContentProvider());
        }

        return composite;
    }

    private void selectUnit(OracleCompileUnit unit)
    {
        infoTable.setInput(Arrays.asList(unit.getCompileActions()));
        UIUtils.packColumns(infoTable.getTable());
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

}
