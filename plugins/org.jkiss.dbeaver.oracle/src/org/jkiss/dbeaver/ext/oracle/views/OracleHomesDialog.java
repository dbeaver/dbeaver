/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.utils.CommonUtils;

/**
 * DriverEditDialog
 */
public class OracleHomesDialog extends TrayDialog
{
    static final Log log = LogFactory.getLog(OracleHomesDialog.class);

    private DBPConnectionInfo connectionInfo;
    private TableViewer pathTable;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;

    private String curFolder = null;

    public OracleHomesDialog(Shell shell, DBPConnectionInfo connectionInfo)
    {
        super(shell);
        this.connectionInfo = connectionInfo;
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
            gd.heightHint = 200;
            libsListGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            libsListGroup.setLayout(layout);
            //gd = new GridData(GridData.FILL_HORIZONTAL);

            // Additional libraries list
            pathTable = new TableViewer(libsListGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            final Table table = pathTable.getTable();
            table.setLayoutData(new GridData(GridData.FILL_BOTH));
            //libsTable.setLinesVisible (true);
            //libsTable.setHeaderVisible (true);
            pathTable.setContentProvider(new ListContentProvider());
            pathTable.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DriverFileDescriptor lib = (DriverFileDescriptor) cell.getElement();
                    cell.setText(lib.getPath());
                    if (lib.getFile().exists()) {
                        cell.setForeground(null);
                    } else {
                        cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    }
                    cell.setImage(
                        !lib.getFile().exists() ?
                            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE) :
                            lib.getFile().isDirectory() ?
                                DBIcon.TREE_FOLDER.getImage() :
                                DBIcon.JAR.getImage());
                }
            });
            pathTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            pathTable.getControl().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event event)
                {

                }
            });

//            libList = new ArrayList<DriverFileDescriptor>();
//            for (DriverFileDescriptor lib : driver.getFiles()) {
//                if (lib.isDisabled() || lib.getType() != DriverFileType.jar) {
//                    continue;
//                }
//                libList.add(lib);
//            }
//            pathTable.setInput(libList);

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
                FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                fd.setText("Choose Oracle Home");
                fd.setFilterPath(curFolder);
                String[] filterExt = {"*.jar", "*.*"};
                fd.setFilterExtensions(filterExt);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    String[] fileNames = fd.getFileNames();
                    if (!CommonUtils.isEmpty(fileNames)) {
//                        File folderFile = new File(curFolder);
//                        changeLibContent();
                    }
                }
            }
        });

        deleteButton = new Button(libsControlGroup, SWT.PUSH);
        deleteButton.setText("&Remove PAth");
        deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
            }
        });
        deleteButton.setEnabled(false);

        upButton = new Button(libsControlGroup, SWT.PUSH);
        upButton.setText("&Up");
        upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upButton.setEnabled(false);
        upButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
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
            }
        });

        return libsGroup;
    }

    protected void okPressed()
    {
        super.okPressed();
    }

}
