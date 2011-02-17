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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
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
        dialogImage = DBeaverActivator.getImageDescriptor("/icons/driver_manager.png").createImage();
        getShell().setImage(dialogImage);

        Composite group = (Composite)super.createDialogArea(parent);
        GridLayout layout = (GridLayout)group.getLayout();
        layout.numColumns = 2;
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 300;
        group.setLayoutData(gd);

        {
            treeControl = new DriverTreeControl(group);
            treeControl.initDrivers(this, provders);
        }
        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            layout = new GridLayout(1, true);
            buttonBar.setLayout(layout);
            buttonBar.setLayoutData(new GridData(GridData.FILL_BOTH));

            newButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            newButton.setText("&New");
            gd = new GridData(GridData.FILL_HORIZONTAL);
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
            gd = new GridData(GridData.FILL_HORIZONTAL);
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
            gd = new GridData(GridData.FILL_HORIZONTAL);
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
        }
        {
            final Composite legend = UIUtils.createPlaceholder(group, 2, 5);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;

            UIUtils.createImageLabel(legend, DBIcon.OVER_CONDITION.getImage());
            UIUtils.createTextLabel(legend, "- User defined driver");

            UIUtils.createImageLabel(legend, DBIcon.OVER_ERROR.getImage());
            UIUtils.createTextLabel(legend, "- Unavailable driver");
        }
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
            DriverEditDialog dialog = new DriverEditDialog(getShell(), selectedDriver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                // Do nothing
            }
            treeControl.refresh();
        }
    }

    private void deleteDriver()
    {
        List<DataSourceDescriptor> usedDS = selectedDriver.getUsedBy();
        if (!usedDS.isEmpty()) {
            StringBuilder message = new StringBuilder("Your can't delete driver '" + selectedDriver.getName() +"' because it's used by next data source(s):");
            for (DataSourceDescriptor ds : usedDS) {
                message.append("\n - ").append(ds.getName());
            }
            UIUtils.showErrorBox(getShell(), "Can't delete driver", message.toString());
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
        if (dialogImage != null) {
            dialogImage.dispose();
            dialogImage = null;
        }
        return super.close();
    }
}