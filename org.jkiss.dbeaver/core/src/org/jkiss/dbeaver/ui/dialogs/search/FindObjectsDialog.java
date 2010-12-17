/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.search;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectListControl;

import java.util.ArrayList;
import java.util.Collection;

public class FindObjectsDialog extends Dialog {

    private static ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        public Object[] getChildren(Object parentElement)
        {
            return null;
        }

        public Object getParent(Object element)
        {
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return false;
        }

        public void dispose()
        {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    private final IWorkbenchPart workbenchPart;
    private java.util.List<DataSourceDescriptor> dataSources = new ArrayList<DataSourceDescriptor>();
    private DBSDataSourceContainer currentDataSource;

    public FindObjectsDialog(IWorkbenchPart workbenchPart, DBSDataSourceContainer currentDataSource)
    {
        super(workbenchPart.getSite().getShell());
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX | SWT.MIN);
        setBlockOnOpen(false);
        this.workbenchPart = workbenchPart;
        this.currentDataSource = currentDataSource;
    }

    protected boolean isResizable() {
    	return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        dataSources.addAll(DBeaverCore.getInstance().getDataSourceRegistry().getDataSources());

        getShell().setText("Find database objects");
        getShell().setImage(DBIcon.FIND.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        {
            Group searchGroup = UIUtils.createControlGroup(composite, "Search", 3, GridData.FILL_HORIZONTAL, 0);
            Text searchText = UIUtils.createLabelText(searchGroup, "Object Name", "");

            Button searchButton = new Button(searchGroup, SWT.PUSH);
            searchButton.setText("Search");
            searchButton.setImage(DBIcon.FIND.getImage());
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            //gd.horizontalSpan = 2;
            searchButton.setLayoutData(gd);

            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(searchButton);
            }

            Composite optionsGroup = new Composite(searchGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            optionsGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(7, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            optionsGroup.setLayout(layout);

            UIUtils.createControlLabel(optionsGroup, "Data Source");
            Combo dsCombo = new Combo(optionsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (int i = 0, dataSourcesSize = dataSources.size(); i < dataSourcesSize; i++) {
                DataSourceDescriptor descriptor = dataSources.get(i);
                dsCombo.add(descriptor.getName());
                if (descriptor == currentDataSource) {
                    dsCombo.select(i);
                }
            }

            UIUtils.createControlLabel(optionsGroup, "Name match");
            Combo matchCombo = new Combo(optionsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            matchCombo.add("Starts with");
            matchCombo.add("Contains");
            matchCombo.add("Mask");
            matchCombo.select(0);

            gd = new GridData();
            gd.widthHint = 50;
            UIUtils.createLabelText(optionsGroup, "Max results", "100", SWT.BORDER, gd);
        }

        {
            Group resultsGroup = UIUtils.createControlGroup(composite, "Results", 1, GridData.FILL_BOTH, 0);
            ObjectListControl itemList = new ObjectListControl(resultsGroup, SWT.BORDER, workbenchPart, CONTENT_PROVIDER) {
                @Override
                protected Object getObjectValue(Object item)
                {
                    return item;
                }

                @Override
                protected Image getObjectImage(Object item)
                {
                    return null;
                }
            };
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 700;
            gd.heightHint = 500;
            itemList.setLayoutData(gd);
        }

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);
    }

}
