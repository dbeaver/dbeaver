/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * EditDriverDialog
 */
public class DriverManagerDialog extends HelpEnabledDialog implements ISelectionChangedListener, IDoubleClickListener {

    private static final String DIALOG_ID = "DBeaver.DriverManagerDialog";//$NON-NLS-1$
    private static final String DEFAULT_DS_PROVIDER = "generic";

    private static final boolean SHOW_EXPORT = false;

    private DataSourceProviderDescriptor selectedProvider;
    private DataSourceProviderDescriptor onlyManagableProvider;
    private String selectedCategory;
    private DriverDescriptor selectedDriver;

    private Button newButton;
    private Button copyButton;
    private Button editButton;
    private Button deleteButton;
    private DriverSelectViewer treeControl;
    private ImageDescriptor dialogImage;
    //private Label driverDescription;
    //private ProgressMonitorPart monitorPart;
    private Text descText;

    public DriverManagerDialog(Shell shell) {
        super(shell, IHelpContextIds.CTX_DRIVER_MANAGER);

    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        List<DBPDataSourceProviderDescriptor> enabledProviders = DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders();
        {
            DBPDataSourceProviderDescriptor manProvider = null;
            for (DBPDataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
                if (provider.isDriversManagable()) {
                    if (manProvider != null) {
                        manProvider = null;
                        break;
                    }
                    manProvider = provider;
                }
            }
            if (manProvider != null) {
                onlyManagableProvider = (DataSourceProviderDescriptor) manProvider;
            }
        }

        getShell().setText(UIConnectionMessages.dialog_driver_manager_title);
        getShell().setMinimumSize(300, 300);
        dialogImage = DBeaverIcons.getImageDescriptor(UIIcon.DRIVER_MANAGER);
        getShell().setImage(dialogImage.createImage());

        Composite group = UIUtils.createPlaceholder((Composite) super.createDialogArea(parent), 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            treeControl = new DriverSelectViewer(group, this, enabledProviders, false, true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 300;
            treeControl.getControl().setLayoutData(gd);
        }

        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            buttonBar.setLayout(new GridLayout(1, false));
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            buttonBar.setLayoutData(gd);

            newButton = UIUtils.createPushButton(buttonBar, UIConnectionMessages.dialog_driver_manager_button_new, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    createDriver();
                }
            });
            newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            copyButton = UIUtils.createPushButton(buttonBar, UIConnectionMessages.dialog_driver_manager_button_copy, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    copyDriver();
                }
            });
            copyButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            editButton = UIUtils.createPushButton(buttonBar, UIConnectionMessages.dialog_driver_manager_button_edit, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editDriver();
                }
            });
            editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            deleteButton = UIUtils.createPushButton(buttonBar, UIConnectionMessages.dialog_driver_manager_button_delete, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    deleteDriver();
                }
            });
            deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Button unDeleteButton = UIUtils.createPushButton(buttonBar, "Un-delete", null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (undeleteDrivers()) {
                        treeControl.refresh();
                    }
                }
            });
            unDeleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                final Composite legend = UIUtils.createPlaceholder(buttonBar, 2, 5);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.verticalIndent = 5;
                gd.horizontalSpan = 2;
                legend.setLayoutData(gd);

                UIUtils.createLabel(legend, DBIcon.OVER_LAMP);
                UIUtils.createLabel(legend, UIConnectionMessages.dialog_driver_manager_label_user_defined);

                UIUtils.createLabel(legend, DBIcon.OVER_ERROR);
                UIUtils.createLabel(legend, UIConnectionMessages.dialog_driver_manager_label_unavailable);

            }

            if (SHOW_EXPORT) {
                UIUtils.createPushButton(buttonBar, "Export", null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        exportDriverList();
                    }
                });
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

    private void exportDriverList() {
        if (!(treeControl.getSelectorViewer() instanceof DriverTreeViewer)) {
            return;
        }
        StringBuilder buf = new StringBuilder();
        DriverTreeViewer driverTreeViewer = (DriverTreeViewer) treeControl.getSelectorViewer();
        List<Object> driverList = (List<Object>) driverTreeViewer.getInput();

        for (Object dObj : driverList) {
            if (dObj instanceof DriverTreeViewer.DriverCategory) {
                DriverTreeViewer.DriverCategory category = (DriverTreeViewer.DriverCategory) dObj;
                buf.append(category.getName()).append("\n");
                for (DriverDescriptor driver : category.getDrivers()) {
                    buf.append("\t");
                    printDriverInfo(buf, driver);
                }
            } else if (dObj instanceof DriverDescriptor) {
                DriverDescriptor driver = (DriverDescriptor) dObj;
                printDriverInfo(buf, driver);
            }
        }

        UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), buf.toString());
    }

    private void printDriverInfo(StringBuilder buf, DriverDescriptor driver) {
        if (driver.isDisabled() || driver.getReplacedBy() != null || driver.isCustom()) {
            return;
        }
        buf.append(driver.getName());

        if (driver.getIcon() == DBIcon.DATABASE_DEFAULT || driver.getIcon() instanceof DBIcon && driver.getIcon().getLocation().endsWith("database.png")) {
            buf.append("\tN/A");
        } else {
            buf.append("\t+");
        }

        buf.append("\n");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            setReturnCode(OK);
            close();
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        setDefaultSelection();

        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DriverDescriptor) {
                this.selectedDriver = (DriverDescriptor) selectedObject;
                this.selectedCategory = selectedDriver.getCategory();
                this.selectedProvider = selectedDriver.getProviderDescriptor();
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                this.selectedProvider = (DataSourceProviderDescriptor) selectedObject;
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
    public void doubleClick(DoubleClickEvent event) {
        if (selectedDriver != null) {
            editDriver();
        }
    }

    private void updateButtons() {
        newButton.setEnabled(onlyManagableProvider != null || (selectedProvider != null && selectedProvider.isDriversManagable()));
        copyButton.setEnabled(selectedDriver != null && selectedDriver.isManagable());
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

    private void createDriver() {
        if (onlyManagableProvider != null || selectedProvider != null) {
            DataSourceProviderDescriptor provider = selectedProvider;
            if (provider == null || !provider.isDriversManagable()) {
                provider = onlyManagableProvider;
            }
            DriverEditDialog dialog = new DriverEditDialog(getShell(), provider, selectedCategory);
            if (dialog.open() == IDialogConstants.OK_ID) {
                treeControl.refresh();
                treeControl.setSelection(new StructuredSelection(dialog.getDriver()));
            }
        }
    }

    private void copyDriver() {
        if (selectedDriver != null) {
            DriverEditDialog dialog = new DriverEditDialog(getShell(), selectedDriver.getProviderDescriptor(), selectedDriver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                treeControl.refresh();
                treeControl.setSelection(new StructuredSelection(dialog.getDriver()));
            }
        }
    }

    private void editDriver() {
        DriverDescriptor driver = selectedDriver;
        if (driver != null) {
            //driver.validateFilesPresence(this);

            DriverEditDialog dialog = new DriverEditDialog(getShell(), driver);
            if (dialog.open() == IDialogConstants.OK_ID) {
                // Do nothing
            }
            treeControl.refresh(driver);
        }
    }

    private void deleteDriver() {
        List<DBPDataSourceContainer> usedDS = DriverUtils.getUsedBy(selectedDriver, DataSourceRegistry.getAllDataSources());
        if (!usedDS.isEmpty()) {
            StringBuilder message = new StringBuilder(NLS.bind(UIConnectionMessages.dialog_driver_manager_message_cant_delete_text, selectedDriver.getName()));
            for (DBPDataSourceContainer ds : usedDS) {
                message.append("\n - ").append(ds.getName());
            }
            UIUtils.showMessageBox(getShell(), UIConnectionMessages.dialog_driver_manager_message_cant_delete_title, message.toString(), SWT.ICON_ERROR);
            return;
        }
        if (UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.dialog_driver_manager_message_delete_driver_title,
            UIConnectionMessages.dialog_driver_manager_message_delete_driver_text + selectedDriver.getName() + "'?")) {
            selectedDriver.getProviderDescriptor().removeDriver(selectedDriver);
            selectedDriver.getProviderDescriptor().getRegistry().saveDrivers();
            treeControl.refresh();
        }
    }

    private boolean undeleteDrivers() {
        List<DriverDescriptor> drivers = new ArrayList<>();

        BaseDialog dialog = new BaseDialog(getShell(), "Restore deleted driver(s)", null) {

            @Override
            protected Composite createDialogArea(Composite parent) {
                final Composite composite = super.createDialogArea(parent);

                Table driverTable = new Table(composite, SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER);
                driverTable.setLayoutData(new GridData(GridData.FILL_BOTH));

                for (DBPDataSourceProviderDescriptor dspd : DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders()) {
                    for (DBPDriver dd : dspd.getDrivers()) {
                        if (dd.isDisabled()) {
                            TableItem item = new TableItem(driverTable, SWT.NONE);
                            item.setImage(DBeaverIcons.getImage(dd.getIcon()));
                            item.setText(dd.getName());
                            item.setData(dd);
                        }
                    }
                }
                driverTable.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (((TableItem)e.item).getChecked()) {
                            drivers.add((DriverDescriptor) e.item.getData());
                        } else {
                            drivers.remove((DriverDescriptor) e.item.getData());
                        }
                    }
                });

                return super.createDialogArea(parent);
            }
        };
        if (dialog.open() == IDialogConstants.OK_ID) {
            for (DriverDescriptor dd : drivers) {
                dd.setDisabled(false);
                dd.getProviderDescriptor().getRegistry().saveDrivers();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
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