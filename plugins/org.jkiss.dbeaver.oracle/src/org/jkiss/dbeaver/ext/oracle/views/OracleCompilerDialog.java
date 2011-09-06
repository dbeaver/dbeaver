/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
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

import java.util.ArrayList;

/**
 * DriverEditDialog
 */
public class OracleCompilerDialog extends TrayDialog
{
    static final Log log = LogFactory.getLog(OracleCompilerDialog.class);

    private java.util.List<OracleCompileUnit> compileUnits;
    private TableViewer unitTable;

    private java.util.List<DriverPathDescriptor> homeDirs = new ArrayList<DriverPathDescriptor>();

    private String curFolder = null;

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
        Composite libsGroup = new Composite(parent, SWT.NONE);
        libsGroup.setLayout(new GridLayout(2, false));
        libsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite libsListGroup = new Composite(libsGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.heightHint = 200;
            libsListGroup.setLayoutData(gd);
            libsListGroup.setLayout(new GridLayout(2, false));

            unitTable = new TableViewer(libsListGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

            {
                final Table table = unitTable.getTable();
                table.setLayoutData(new GridData(GridData.FILL_VERTICAL));
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
            unitTable.setContentProvider(new ListContentProvider());
            unitTable.setInput(compileUnits);
            UIUtils.packColumns(unitTable.getTable());
        }

        return libsGroup;
    }

    protected void okPressed()
    {
        super.okPressed();
    }

}
