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
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverPathDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;

import java.util.ArrayList;

/**
 * DriverEditDialog
 */
public class OracleHomesDialog extends TrayDialog
{
    static final Log log = LogFactory.getLog(OracleHomesDialog.class);

    private DriverDescriptor driver;
    private TableViewer pathTable;
    private Button deleteButton;
    private Button toggleButton;
    private Button upButton;
    private Button downButton;

    private java.util.List<DriverPathDescriptor> homeDirs = new ArrayList<DriverPathDescriptor>();

    private String curFolder = null;

    public OracleHomesDialog(Shell shell, DBPDriver driver)
    {
        super(shell);
        this.driver = (DriverDescriptor) driver;
        this.homeDirs = new ArrayList<DriverPathDescriptor>(((DriverDescriptor) driver).getPathList());
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Oracle Home Edit");

        GridData gd;
        Composite libsGroup = new Composite(parent, SWT.NONE);
        libsGroup.setLayout(new GridLayout(2, false));
        libsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        //ListViewer list = new ListViewer(libsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            Composite libsListGroup = new Composite(libsGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.heightHint = 200;
            libsListGroup.setLayoutData(gd);
            libsListGroup.setLayout(new FillLayout());
            //gd = new GridData(GridData.FILL_HORIZONTAL);

            // Additional libraries list
            pathTable = new TableViewer(libsListGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

            {
                final Table table = pathTable.getTable();
                table.setLinesVisible(true);
                table.setHeaderVisible(true);
            }

            final TableViewerColumn pathColumn = UIUtils.createTableViewerColumn(pathTable, SWT.NONE, "Path");
            pathColumn.setLabelProvider(new PathLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DriverPathDescriptor path = (DriverPathDescriptor) cell.getElement();
                    cell.setText(path.getPath());
                }
            });
            final TableViewerColumn versionColumn = UIUtils.createTableViewerColumn(pathTable, SWT.NONE, "Version");
            versionColumn.setLabelProvider(new PathLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DriverPathDescriptor path = (DriverPathDescriptor) cell.getElement();
                    cell.setText(path.getComment());
                }
            });

            pathTable.setContentProvider(new ListContentProvider());
            pathTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            pathTable.getControl().addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event)
                {

                }
            });
            pathTable.setInput(this.homeDirs);

            UIUtils.packColumns(pathTable.getTable(), true);

            pathTable.addSelectionChangedListener(new ISelectionChangedListener() {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    updateButtons();
                }
            });
        }

        Composite libsControlGroup = new Composite(libsGroup, SWT.TOP);
        libsControlGroup.setLayout(new GridLayout(1, true));
        libsControlGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Button newButton = new Button(libsControlGroup, SWT.PUSH);
        newButton.setText("&Add Path");
        newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        newButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN | SWT.MULTI);
                fd.setText("Choose Oracle Home");
                fd.setFilterPath(curFolder);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = selected;
                    addOracleHome(selected);
                }
            }
        });

        deleteButton = new Button(libsControlGroup, SWT.PUSH);
        deleteButton.setText("&Remove Path");
        deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                final DriverPathDescriptor path = getSelectedPath();
                if (path != null) {
                    removeOracleHome(path);
                }
            }
        });
        deleteButton.setEnabled(false);

        toggleButton = new Button(libsControlGroup, SWT.PUSH);
        toggleButton.setText("Toggle");
        toggleButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toggleButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                final DriverPathDescriptor path = getSelectedPath();
                if (path != null) {
                    toggleOracleHome(path);
                }
            }
        });
        toggleButton.setEnabled(false);

        upButton = new Button(libsControlGroup, SWT.PUSH);
        upButton.setText("&Up");
        upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upButton.setEnabled(false);
        upButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                final DriverPathDescriptor path = getSelectedPath();
                if (path != null) {
                    moveOracleHome(path, false);
                }
            }
        });

        downButton = new Button(libsControlGroup, SWT.PUSH);
        downButton.setText("Do&wn");
        downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downButton.setEnabled(false);
        downButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                final DriverPathDescriptor path = getSelectedPath();
                if (path != null) {
                    moveOracleHome(path, true);
                }
            }
        });

        updateButtons();

        return libsGroup;
    }

    private void updateButtons()
    {
        final DriverPathDescriptor path = getSelectedPath();
        if (path == null) {
            deleteButton.setEnabled(false);
            toggleButton.setEnabled(false);
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        } else {
            deleteButton.setEnabled(true);
            toggleButton.setEnabled(true);
            if (path == homeDirs.get(0)) {
                upButton.setEnabled(false);
            } else {
                upButton.setEnabled(true);
            }
            if (path == homeDirs.get(homeDirs.size() - 1)) {
                downButton.setEnabled(false);
            } else {
                downButton.setEnabled(true);
            }
        }
    }

    DriverPathDescriptor getSelectedPath()
    {
        final ISelection selection = pathTable.getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            return (DriverPathDescriptor) ((IStructuredSelection) selection).getFirstElement();
        }
        return null;
    }

    private void addOracleHome(String path)
    {
        DriverPathDescriptor driverPath = new DriverPathDescriptor();
        driverPath.setPath(path);
        driverPath.setComment("ORAHOME");
        driverPath.setEnabled(true);
        homeDirs.add(driverPath);
        pathTable.refresh();
    }

    private void moveOracleHome(DriverPathDescriptor path, boolean down)
    {

    }

    private void removeOracleHome(DriverPathDescriptor path)
    {
        homeDirs.remove(path);
        pathTable.refresh();
    }

    private void toggleOracleHome(DriverPathDescriptor path)
    {
        path.setEnabled(!path.isEnabled());
        pathTable.update(path, null);
    }

    protected void okPressed()
    {
        driver.setPathList(homeDirs);
        driver.setModified(true);
        DBeaverCore.getInstance().getDataSourceProviderRegistry().saveDrivers();
        super.okPressed();
    }

    private class PathLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell)
        {
            DriverPathDescriptor path = (DriverPathDescriptor) cell.getElement();
            cell.setText(path.getPath());
            if (!path.isEnabled()) {
                cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
            } else {
                cell.setForeground(null);
            }
        }
    }
}
