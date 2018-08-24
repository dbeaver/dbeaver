/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ClientHomesPanel
 */
public class ClientHomesPanel extends Composite
{
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
        DBPNativeClientLocation home;
        boolean isProvided;
        boolean isDefault;

        HomeInfo(DBPNativeClientLocation home)
        {
            this.home = home;
        }
    }

    public ClientHomesPanel(
        Composite parent,
        int style)
    {
        super(parent, style);

        fontBold = UIUtils.makeBoldFont(parent.getFont());
        fontItalic = UIUtils.modifyFont(parent.getFont(), SWT.ITALIC);
        addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(fontBold);
                UIUtils.dispose(fontItalic);
            }
        });

        GridLayout layout = new GridLayout(2, false);
        setLayout(layout);

        Composite listGroup = UIUtils.createPlaceholder(this, 1, 5);
        listGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        ((GridData)(listGroup.getLayoutData())).minimumWidth = 200;
        homesTable = new Table(listGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        homesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = homesTable.getSelection();
                if (ArrayUtils.isEmpty(selection)) {
                    selectHome(null);
                } else {
                    selectHome((HomeInfo)selection[0].getData());
                }
            }
        });
        Composite buttonsGroup = UIUtils.createPlaceholder(listGroup, 2, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText(CoreMessages.controls_client_homes_panel_button_add_home);
        addButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addClientHome();
            }
        });
        removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText(CoreMessages.controls_client_homes_panel_button_remove_home);
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = homesTable.getSelection();
                if (!ArrayUtils.isEmpty(selection)) {
                    removeClientHome();
                }
            }
        });

        Group infoGroup = UIUtils.createControlGroup(this, CoreMessages.controls_client_homes_panel_group_information, 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        ((GridData)(infoGroup.getLayoutData())).minimumWidth = 300;
        idText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_id, null, SWT.BORDER | SWT.READ_ONLY);
        pathText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_path, null, SWT.BORDER | SWT.READ_ONLY);
        nameText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_name, null, SWT.BORDER | SWT.READ_ONLY);
        productNameText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_product_name, null, SWT.BORDER | SWT.READ_ONLY);
        productVersionText = UIUtils.createLabelText(infoGroup, CoreMessages.controls_client_homes_panel_label_product_version, null, SWT.BORDER | SWT.READ_ONLY);
    }

    private void removeClientHome()
    {
        int selIndex = homesTable.getSelectionIndex();
        HomeInfo info = (HomeInfo) homesTable.getItem(selIndex).getData();
        if (!info.isProvided) {
            if (UIUtils.confirmAction(
                getShell(),
                CoreMessages.controls_client_homes_panel_confirm_remove_home_title,
                NLS.bind(CoreMessages.controls_client_homes_panel_confirm_remove_home_text, info.home.getHomeId())))
            {
                homesTable.remove(selIndex);
                selectHome(null);
            }
        }
    }

    private void addClientHome()
    {
        DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
        if (lastHomeDirectory != null) {
            directoryDialog.setFilterPath(lastHomeDirectory);
        }
        String homeId = directoryDialog.open();
        if (homeId == null) {
            return;
        }
        lastHomeDirectory = homeId;
        DBPNativeClientLocationManager clientManager = driver.getClientManager();
        if (clientManager != null) {
            createHomeItem(clientManager, homeId, false);
        }
    }

    private void selectHome(HomeInfo home)
    {
        removeButton.setEnabled(home != null && !home.isProvided);
        idText.setText(home == null ? "" : CommonUtils.notEmpty(home.home.getHomeId())); //$NON-NLS-1$
        pathText.setText(home == null ? "" : home.home.getHomePath().getAbsolutePath()); //$NON-NLS-1$
        nameText.setText(home == null ? "" : CommonUtils.notEmpty(home.home.getDisplayName())); //$NON-NLS-1$
        try {
            productNameText.setText(home == null ? "" : CommonUtils.notEmpty(home.home.getProductName())); //$NON-NLS-1$
        } catch (DBException e) {
            log.warn(e);
        }
        try {
            productVersionText.setText(home == null ? "" : CommonUtils.notEmpty(home.home.getProductVersion())); //$NON-NLS-1$
        } catch (DBException e) {
            log.warn(e);
        }
    }

    public Collection<String> getHomeIds()
    {
        java.util.List<String> homes = new ArrayList<>();
        for (TableItem item : homesTable.getItems()) {
            homes.add(((HomeInfo)item.getData()).home.getHomeId());
        }
        return homes;
    }

    public void loadHomes(DBPDriver driver)
    {
        homesTable.removeAll();
        selectHome(null);

        this.driver = driver;
        DBPNativeClientLocationManager clientManager = this.driver.getClientManager();
        if (clientManager == null) {
            log.error("Client manager is not supported by driver '" + driver.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        Set<String> providedHomes = new LinkedHashSet<>(
            clientManager.findNativeClientHomeIds());
        Set<String> allHomes = new LinkedHashSet<>(providedHomes);
        allHomes.addAll(driver.getClientHomeIds());

        for (String homeId : allHomes) {
            TableItem item = createHomeItem(clientManager, homeId, providedHomes.contains(homeId));
            if (item != null) {
                HomeInfo homeInfo = (HomeInfo) item.getData();
                if (homeInfo.isDefault) {
                    homesTable.setSelection(homesTable.indexOf(item));
                    selectHome(homeInfo);
                }
            }
        }
    }

    private TableItem createHomeItem(DBPNativeClientLocationManager clientManager, String homeId, boolean provided)
    {
        DBPNativeClientLocation home;
        try {
            home = clientManager.getNativeClientHome(homeId);
            if (home == null) {
                log.warn("Home '" + homeId + "' is not supported"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        } catch (Exception e) {
            log.error(e);
            return null;
        }
        HomeInfo homeInfo = new HomeInfo(home);
        homeInfo.isProvided = provided;
        homeInfo.isDefault = home.getHomeId().equals(clientManager.getDefaultNativeClientHomeId());
        TableItem homeItem = new TableItem(homesTable, SWT.NONE);
        homeItem.setText(home.getDisplayName());
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

    private String getSelectedHome()
    {
        TableItem[] selection = homesTable.getSelection();
        if (ArrayUtils.isEmpty(selection)) {
            return null;
        } else {
            return ((HomeInfo)selection[0].getData()).home.getHomeId();
        }
    }

    private static class ChooserDialog extends org.eclipse.jface.dialogs.Dialog {
        private DBPDriver driver;
        private ClientHomesPanel panel;
        private String selectedHome;

        protected ChooserDialog(Shell parentShell, DBPDriver driver)
        {
            super(parentShell);
            this.driver = driver;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText(CoreMessages.controls_client_homes_panel_dialog_title);

            panel = new ClientHomesPanel(parent, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            //gd.widthHint = 500;
            panel.setLayoutData(gd);
            panel.loadHomes(driver);

            return parent;
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected void buttonPressed(int buttonId)
        {
            if (IDialogConstants.OK_ID == buttonId) {
                selectedHome = panel.getSelectedHome();
                if (driver instanceof DriverDescriptor) {
                    ((DriverDescriptor) driver).setClientHomeIds(panel.getHomeIds());
                    ((DriverDescriptor) driver).getProviderDescriptor().getRegistry().saveDrivers();
                }
            }
            super.buttonPressed(buttonId);
        }
    }

    public static String chooseClientHome(Shell parent, DBPDriver driver)
    {
        ChooserDialog dialog = new ChooserDialog(parent, driver);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.selectedHome;
        } else {
            return null;
        }
    }

}