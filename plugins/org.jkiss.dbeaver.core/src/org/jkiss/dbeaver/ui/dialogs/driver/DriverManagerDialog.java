/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DriverTreeControl;

import java.util.List;

/**
 * EditDriverDialog
 */
public class DriverManagerDialog extends Dialog implements ISelectionChangedListener, IDoubleClickListener {

    private DataSourceProviderDescriptor selectedProvider;
    private DataSourceProviderDescriptor onlyManagableProvider;
    private DriverDescriptor selectedDriver;

    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    private DriverTreeControl treeControl;
    private Image dialogImage;

    public DriverManagerDialog(Shell shell)
    {
        super(shell);
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        List<DataSourceProviderDescriptor> provders = DataSourceProviderRegistry.getDefault().getDataSourceProviders();
        {
            DataSourceProviderDescriptor manProvider = null;
            for (DataSourceProviderDescriptor provider : provders) {
                if (provider.isDriversManagable()) {
                    if (manProvider != null) {
                        manProvider = null;
                        break;
                    }
                    manProvider = provider;
                }
            }
            if (manProvider != null) {
                onlyManagableProvider = manProvider;
            }
        }

        getShell().setText("Driver Manager");
        getShell().setMinimumSize(300, 300);
        dialogImage = DBeaverActivator.getImageDescriptor("/icons/driver_manager.png").createImage();
        getShell().setImage(dialogImage);

        Composite group = UIUtils.createPlaceholder((Composite) super.createDialogArea(parent), 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            treeControl = new DriverTreeControl(group);
            treeControl.initDrivers(this, provders);
            treeControl.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            buttonBar.setLayout(new GridLayout(1, false));
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            gd.minimumWidth = 100;
            buttonBar.setLayoutData(gd);

            newButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            newButton.setText("&New");
            gd = new GridData(GridData.BEGINNING);
            gd.widthHint = 100;
            newButton.setLayoutData(gd);
            newButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    createDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            editButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            editButton.setText("&Edit ...");
            gd = new GridData(GridData.BEGINNING);
            gd.widthHint = 100;
            editButton.setLayoutData(gd);
            editButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    editDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            deleteButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            deleteButton.setText("&Delete");
            gd = new GridData(GridData.BEGINNING);
            gd.widthHint = 100;
            deleteButton.setLayoutData(gd);
            deleteButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    deleteDriver();
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            {
                final Composite legend = UIUtils.createPlaceholder(buttonBar, 2, 5);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.verticalIndent = 5;
                gd.horizontalSpan = 2;
                legend.setLayoutData(gd);

                UIUtils.createImageLabel(legend, DBIcon.OVER_CONDITION.getImage());
                UIUtils.createTextLabel(legend, "- User defined");

                UIUtils.createImageLabel(legend, DBIcon.OVER_ERROR.getImage());
                UIUtils.createTextLabel(legend, "- Unavailable");
            }
        }
/*
        {
            Composite descBar = UIUtils.createPlaceholder(group, 1, 5);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            descBar.setLayoutData(gd);

            Text text = new Text(descBar, SWT.READ_ONLY | SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            //gd.verticalIndent = 5;
            text.setLayoutData(gd);
        }
*/
        return group;
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            true);
    }

    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            setReturnCode(OK);
            close();
        }
    }

    public void selectionChanged(SelectionChangedEvent event)
    {
        this.selectedDriver = null;
        this.selectedProvider = null;
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DriverDescriptor) {
                selectedDriver = (DriverDescriptor) selectedObject;
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                selectedProvider = (DataSourceProviderDescriptor)selectedObject;
            }
        }
        this.updateButtons();
    }

    public void doubleClick(DoubleClickEvent event)
    {
        if (selectedDriver != null) {
            editDriver();
        }
    }

    private void updateButtons()
    {
        newButton.setEnabled(onlyManagableProvider != null || (selectedProvider != null && selectedProvider.isDriversManagable()));
        editButton.setEnabled(selectedDriver != null);
        deleteButton.setEnabled(selectedDriver != null && selectedDriver.getProviderDescriptor().isDriversManagable());
    }

    private void createDriver()
    {
        if (onlyManagableProvider != null || selectedProvider != null) {
            DataSourceProviderDescriptor provider = selectedProvider;
            if (provider == null || !provider.isDriversManagable()) {
                provider = onlyManagableProvider;
            }
            DriverEditDialog dialog = new DriverEditDialog(getShell(), provider);
            if (dialog.open() == IDialogConstants.OK_ID) {
                treeControl.refresh(provider);
            }
        }
    }

    private void editDriver()
    {
        if (selectedDriver != null) {
            selectedDriver.validateFilesPresence();

            DriverEditDialog dialog = new DriverEditDialog(getShell(), selectedDriver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                // Do nothing
            }
            treeControl.refresh(selectedDriver);
        }
    }

    private void deleteDriver()
    {
        List<DataSourceDescriptor> usedDS = selectedDriver.getUsedBy();
        if (!usedDS.isEmpty()) {
            StringBuilder message = new StringBuilder("Your can't delete driver '" + selectedDriver.getName() +"' because it's used by following datasource(s):");
            for (DataSourceDescriptor ds : usedDS) {
                message.append("\n - ").append(ds.getName());
            }
            UIUtils.showMessageBox(getShell(), "Can't delete driver", message.toString(), SWT.ICON_ERROR);
            return;
        }
        if (UIUtils.confirmAction(
            getShell(),
            "Delete driver",
            "Are you sure you want to delete driver '" + selectedDriver.getName() + "'?"))
        {
            selectedDriver.getProviderDescriptor().removeDriver(selectedDriver);
            selectedDriver.getProviderDescriptor().getRegistry().saveDrivers();
            treeControl.refresh();
        }
    }

    @Override
    public boolean close()
    {
        UIUtils.dispose(dialogImage);
        return super.close();
    }
}