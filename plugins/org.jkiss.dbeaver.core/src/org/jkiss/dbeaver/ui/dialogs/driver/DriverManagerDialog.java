/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * EditDriverDialog
 */
public class DriverManagerDialog extends HelpEnabledDialog implements ISelectionChangedListener, IDoubleClickListener {

    private static final String DIALOG_ID = "DBeaver.DriverManagerDialog";//$NON-NLS-1$
    private static final String DEFAULT_DS_PROVIDER = "generic";

    private DataSourceProviderDescriptor selectedProvider;
    private DataSourceProviderDescriptor onlyManagableProvider;
    private String selectedCategory;
    private DriverDescriptor selectedDriver;

    private Button newButton;
    private Button editButton;
    private Button deleteButton;
    private DriverTreeControl treeControl;
    private Image dialogImage;
    //private Label driverDescription;
    //private ProgressMonitorPart monitorPart;
    private Text descText;

    public DriverManagerDialog(Shell shell)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_MANAGER);

    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        List<DataSourceProviderDescriptor> enabledProviders = DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders();
        {
            DataSourceProviderDescriptor manProvider = null;
            for (DataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
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

        getShell().setText(CoreMessages.dialog_driver_manager_title);
        getShell().setMinimumSize(300, 300);
        dialogImage = DBeaverActivator.getImageDescriptor("/icons/driver_manager.png").createImage(); //$NON-NLS-1$
        getShell().setImage(dialogImage);

        Composite group = UIUtils.createPlaceholder((Composite) super.createDialogArea(parent), 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            treeControl = new DriverTreeControl(group, this, enabledProviders, false);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 300;
            treeControl.setLayoutData(gd);
        }

        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            buttonBar.setLayout(new GridLayout(1, false));
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            gd.minimumWidth = 100;
            buttonBar.setLayoutData(gd);

            newButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            newButton.setText(CoreMessages.dialog_driver_manager_button_new);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 100;
            newButton.setLayoutData(gd);
            newButton.addSelectionListener(new SelectionListener()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    createDriver();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            editButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            editButton.setText(CoreMessages.dialog_driver_manager_button_edit);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 100;
            editButton.setLayoutData(gd);
            editButton.addSelectionListener(new SelectionListener()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    editDriver();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            deleteButton = new Button(buttonBar, SWT.FLAT | SWT.PUSH);
            deleteButton.setText(CoreMessages.dialog_driver_manager_button_delete);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 100;
            deleteButton.setLayoutData(gd);
            deleteButton.addSelectionListener(new SelectionListener()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    deleteDriver();
                }

                @Override
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

                UIUtils.createLabel(legend, DBIcon.OVER_LAMP);
                UIUtils.createLabel(legend, CoreMessages.dialog_driver_manager_label_user_defined);

                UIUtils.createLabel(legend, DBIcon.OVER_ERROR);
                UIUtils.createLabel(legend, CoreMessages.dialog_driver_manager_label_unavailable);

            }
        }

        descText = new Text(group, SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 5;
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        descText.setLayoutData(gd);
/*
        monitorPart = new ProgressMonitorPart(group, null, true);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 5;
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        monitorPart.setLayoutData(gd);
        monitorPart.setVisible(false);
*/

        setDefaultSelection();
        this.updateButtons();
        return group;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            true);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            setReturnCode(OK);
            close();
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        setDefaultSelection();

        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DriverDescriptor) {
                this.selectedDriver = (DriverDescriptor) selectedObject;
                this.selectedCategory = selectedDriver.getCategory();
                this.selectedProvider = selectedDriver.getProviderDescriptor();
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                this.selectedProvider = (DataSourceProviderDescriptor)selectedObject;
            } else if (selectedObject instanceof DriverTreeViewer.DriverCategory) {
                //this.selectedProvider = null;
                this.selectedCategory = ((DriverTreeViewer.DriverCategory) selectedObject).getName();
            }
        }
            //super.updateStatus(new Status(Status.INFO, DBeaverConstants.PLUGIN_ID, selectedDriver == null ? "" : selectedDriver.getDescription()));
        this.updateButtons();
    }

    private void setDefaultSelection() {
        this.selectedDriver = null;
        this.selectedProvider = DataSourceProviderRegistry.getInstance().getDataSourceProvider(DEFAULT_DS_PROVIDER);
        this.selectedCategory = null;
    }

    @Override
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

        if (selectedDriver != null) {
            descText.setText(CommonUtils.toString(selectedDriver.getDescription()));
        } else if (selectedCategory != null) {
            descText.setText(selectedCategory + " drivers");
        } else if (selectedProvider != null) {
            descText.setText(selectedProvider.getName() + " provider");
        } else {
            descText.setText("");
        }

/*
        if (selectedDriver != null) {
            monitorPart.setTaskName(CommonUtils.toString(selectedDriver.getDescription()));
        } else if (selectedCategory != null) {
            monitorPart.setTaskName(selectedCategory + " drivers");
        } else if (selectedProvider != null) {
            monitorPart.setTaskName(selectedProvider.getName() + " provider");
        } else {
            monitorPart.setTaskName("");
        }
*/
    }

    private void createDriver()
    {
        if (onlyManagableProvider != null || selectedProvider != null) {
            DataSourceProviderDescriptor provider = selectedProvider;
            if (provider == null || !provider.isDriversManagable()) {
                provider = onlyManagableProvider;
            }
            DriverEditDialog dialog = new DriverEditDialog(getShell(), provider, selectedCategory);
            if (dialog.open() == IDialogConstants.OK_ID) {
                treeControl.getViewer().refresh();
            }
        }
    }

    private void editDriver()
    {
        DriverDescriptor driver = selectedDriver;
        if (driver != null) {
            //driver.validateFilesPresence(this);

            DriverEditDialog dialog = new DriverEditDialog(getShell(), driver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                // Do nothing
            }
            treeControl.getViewer().refresh(driver);
        }
    }

    private void deleteDriver()
    {
        List<DataSourceDescriptor> usedDS = selectedDriver.getUsedBy();
        if (!usedDS.isEmpty()) {
            StringBuilder message = new StringBuilder(NLS.bind(CoreMessages.dialog_driver_manager_message_cant_delete_text, selectedDriver.getName()));
            for (DataSourceDescriptor ds : usedDS) {
                message.append("\n - ").append(ds.getName());
            }
            UIUtils.showMessageBox(getShell(), CoreMessages.dialog_driver_manager_message_cant_delete_title, message.toString(), SWT.ICON_ERROR);
            return;
        }
        if (UIUtils.confirmAction(
            getShell(),
            CoreMessages.dialog_driver_manager_message_delete_driver_title,
            CoreMessages.dialog_driver_manager_message_delete_driver_text + selectedDriver.getName() + "'?"))
        {
            selectedDriver.getProviderDescriptor().removeDriver(selectedDriver);
            selectedDriver.getProviderDescriptor().getRegistry().saveDrivers();
            treeControl.getViewer().refresh();
        }
    }

    @Override
    public boolean close()
    {
        UIUtils.dispose(dialogImage);
        return super.close();
    }

/*    @Override
    public void run(boolean fork, boolean cancelable, final DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
    {
        // Code copied from WizardDialog
        if (monitorPart != null) {
            monitorPart.setVisible(true);
            monitorPart.layout();
            monitorPart.attachToCancelComponent(null);
        }
        // The operation can only be canceled if it is executed in a separate
        // thread.
        // Otherwise the UI is blocked anyway.
        try {
            ModalContext.run(
                new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        runnable.run(new DefaultProgressMonitor(monitor));
                    }
                },
                true,
                monitorPart,
                getShell().getDisplay());
        } finally {
            // explicitly invoke done() on our progress monitor so that its
            // label does not spill over to the next invocation, see bug 271530
            if (monitorPart != null) {
                monitorPart.done();
                monitorPart.setVisible(false);
            }
        }
    }*/
}