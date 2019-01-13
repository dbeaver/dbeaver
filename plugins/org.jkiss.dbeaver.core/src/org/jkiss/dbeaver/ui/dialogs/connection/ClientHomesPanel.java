/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.registry.driver.LocalNativeClientLocation;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.RemoteNativeClientLocation;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ClientHomesPanel
 */
public class ClientHomesPanel extends Composite {
    private static final Log log = Log.getLog(ClientHomesPanel.class);

    private static String lastHomeDirectory;

    private Table homesTable;
    private Text idText;
    private Text pathText;
    private Text nameText;
    private Text productNameText;
    private Text productVersionText;
    private Button removeButton;
    private Font fontBold;
    private Font fontItalic;

    private DBPDriver driver;

    static class HomeInfo {
        DBPNativeClientLocation location;
        boolean isProvided;
        boolean isDefault;
        public boolean isValidated;

        HomeInfo(DBPNativeClientLocation location) {
            this.location = location;
        }
    }

    public ClientHomesPanel(
        Composite parent,
        int style) {
        super(parent, style);

        fontBold = UIUtils.makeBoldFont(parent.getFont());
        fontItalic = UIUtils.modifyFont(parent.getFont(), SWT.ITALIC);
        addDisposeListener(e -> {
            UIUtils.dispose(fontBold);
            UIUtils.dispose(fontItalic);
        });

        GridLayout layout = new GridLayout(2, false);
        setLayout(layout);

        Composite listGroup = UIUtils.createPlaceholder(this, 1, 5);
        listGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        ((GridData) (listGroup.getLayoutData())).minimumWidth = 200;
        homesTable = new Table(listGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        homesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] selection = homesTable.getSelection();
                if (ArrayUtils.isEmpty(selection)) {
                    selectHome(null);
                } else {
                    selectHome((HomeInfo) selection[0].getData());
                }
            }
        });
        Composite buttonsGroup = UIUtils.createPlaceholder(listGroup, 2, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(CoreMessages.controls_client_homes_panel_button_add_home);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addClientHome();
            }
        });
        removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(CoreMessages.controls_client_homes_panel_button_remove_home);
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] selection = homesTable.getSelection();
                if (!ArrayUtils.isEmpty(selection)) {
                    removeClientHome();
                }
            }
        });

        Group infoGroup = UIUtils.createControlGroup(this, CoreMessages.controls_client_homes_panel_group_information, 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        ((GridData) (infoGroup.getLayoutData())).minimumWidth = 300;
        idText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_id, null, SWT.BORDER | SWT.READ_ONLY);
        pathText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_path, null, SWT.BORDER | SWT.READ_ONLY);
        nameText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_name, null, SWT.BORDER | SWT.READ_ONLY);
        productNameText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_product_name, null, SWT.BORDER | SWT.READ_ONLY);
        productVersionText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_product_version, null, SWT.BORDER | SWT.READ_ONLY);
    }

    private void removeClientHome() {
        int selIndex = homesTable.getSelectionIndex();
        HomeInfo info = (HomeInfo) homesTable.getItem(selIndex).getData();
        if (!info.isProvided) {
            if (UIUtils.confirmAction(
                getShell(),
                CoreMessages.controls_client_homes_panel_confirm_remove_home_title,
                NLS.bind(CoreMessages.controls_client_homes_panel_confirm_remove_home_text, info.location.getName()))) {
                homesTable.remove(selIndex);
                selectHome(null);
            }
        }
    }

    private void addClientHome() {
        DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
        if (lastHomeDirectory != null) {
            directoryDialog.setFilterPath(lastHomeDirectory);
        }
        String homeId = directoryDialog.open();
        if (homeId == null) {
            return;
        }
        lastHomeDirectory = homeId;
        DBPNativeClientLocationManager clientManager = driver.getNativeClientManager();
        if (clientManager != null) {
            createHomeItem(
                clientManager,
                new LocalNativeClientLocation(homeId, homeId),
                false);
        }
    }

    private void selectHome(HomeInfo home) {
        removeButton.setEnabled(home != null && !home.isProvided);
        idText.setText(home == null ? "" : CommonUtils.notEmpty(home.location.getName())); //$NON-NLS-1$
        pathText.setText(home == null ? "" : home.location.getPath().getAbsolutePath()); //$NON-NLS-1$
        nameText.setText(home == null ? "" : CommonUtils.notEmpty(home.location.getDisplayName())); //$NON-NLS-1$
        if (home != null && !home.isValidated) {
            try {
                UIUtils.runInProgressDialog(monitor -> {
                    try {
                        home.location.validateFilesPresence(monitor);
                        home.isValidated = true;
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Client download", "Failed to download client files", e.getTargetException());
            }
        }
        try {
            productNameText.setText(home == null ? "" : CommonUtils.notEmpty(driver.getNativeClientManager().getProductName(home.location))); //$NON-NLS-1$
        } catch (DBException e) {
            log.warn(e);
        }
        try {
            productVersionText.setText(home == null ? "" : CommonUtils.notEmpty(driver.getNativeClientManager().getProductVersion(home.location))); //$NON-NLS-1$
        } catch (DBException e) {
            log.warn(e);
        }
    }

    public Collection<DBPNativeClientLocation> getLocalLocations() {
        List<DBPNativeClientLocation> homes = new ArrayList<>();
        for (TableItem item : homesTable.getItems()) {
            HomeInfo homeInfo = (HomeInfo) item.getData();
            if (!homeInfo.isProvided) {
                homes.add(homeInfo.location);
            }
        }
        return homes;
    }

    public void loadHomes(DBPDriver driver) {
        homesTable.removeAll();
        selectHome(null);

        this.driver = driver;
        DBPNativeClientLocationManager clientManager = this.driver.getNativeClientManager();
        if (clientManager == null) {
            log.debug("Client manager is not supported by driver '" + driver.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        Set<DBPNativeClientLocation> providedHomes = new LinkedHashSet<>();
        if (clientManager != null) {
            providedHomes.addAll(clientManager.findLocalClientLocations());
        }
        Set<DBPNativeClientLocation> allHomes = new LinkedHashSet<>();
        allHomes.addAll(driver.getNativeClientLocations());
        allHomes.addAll(providedHomes);

        for (DBPNativeClientLocation home : allHomes) {
            TableItem item = createHomeItem(clientManager, home, home instanceof RemoteNativeClientLocation || providedHomes.contains(home));
            if (item != null) {
                HomeInfo homeInfo = (HomeInfo) item.getData();
                if (homeInfo.isDefault) {
                    homesTable.setSelection(homesTable.indexOf(item));
                    selectHome(homeInfo);
                }
            }
        }
    }

    private TableItem createHomeItem(@NotNull DBPNativeClientLocationManager clientManager, @NotNull DBPNativeClientLocation clientLocation, boolean provided) {
        DBPNativeClientLocation defaultLocalClientLocation = clientManager.getDefaultLocalClientLocation();
        if (defaultLocalClientLocation == null) {
            List<DBPNativeClientLocation> driverLocations = driver.getNativeClientLocations();
            if (!CommonUtils.isEmpty(driverLocations)) {
                defaultLocalClientLocation = driverLocations.get(0);
            }
        }
        HomeInfo homeInfo = new HomeInfo(clientLocation);
        homeInfo.isProvided = provided;
        homeInfo.isDefault = defaultLocalClientLocation != null && clientLocation.getName().equals(defaultLocalClientLocation.getName());
        TableItem homeItem = new TableItem(homesTable, SWT.NONE);
        homeItem.setText(clientLocation.getDisplayName());
        homeItem.setImage(DBeaverIcons.getImage(UIIcon.HOME));
        homeItem.setData(homeInfo);
        if (!homeInfo.isProvided) {
            homeItem.setFont(fontItalic);
        } else {
            if (homeInfo.isDefault) {
                homeItem.setFont(fontBold);
            }
        }
        return homeItem;
    }

    private String getSelectedHome() {
        TableItem[] selection = homesTable.getSelection();
        if (ArrayUtils.isEmpty(selection)) {
            return null;
        } else {
            return ((HomeInfo) selection[0].getData()).location.getName();
        }
    }

    private static class ChooserDialog extends org.eclipse.jface.dialogs.Dialog {
        private DBPDriver driver;
        private ClientHomesPanel panel;
        private String selectedHome;

        ChooserDialog(Shell parentShell, DBPDriver driver) {
            super(parentShell);
            this.driver = driver;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(CoreMessages.controls_client_homes_panel_dialog_title);

            panel = new ClientHomesPanel(parent, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            //gd.widthHint = 500;
            panel.setLayoutData(gd);
            panel.loadHomes(driver);

            return parent;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected void buttonPressed(int buttonId) {
            if (IDialogConstants.OK_ID == buttonId) {
                selectedHome = panel.getSelectedHome();
                if (driver instanceof DriverDescriptor) {
                    ((DriverDescriptor) driver).setNativeClientLocations(panel.getLocalLocations());
                    ((DriverDescriptor) driver).getProviderDescriptor().getRegistry().saveDrivers();
                }
            }
            super.buttonPressed(buttonId);
        }
    }

    static String chooseClientHome(Shell parent, DBPDriver driver) {
        ChooserDialog dialog = new ChooserDialog(parent, driver);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.selectedHome;
        } else {
            return null;
        }
    }

}