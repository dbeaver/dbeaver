/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.*;
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
import org.jkiss.dbeaver.model.DBPClientHome;
import org.jkiss.dbeaver.model.DBPClientManager;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
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
    static final Log log = LogFactory.getLog(ClientHomesPanel.class);

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
        DBPClientHome home;
        boolean isProvided;
        boolean isDefault;

        HomeInfo(DBPClientHome home)
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
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(fontBold);
                UIUtils.dispose(fontItalic);
            }
        });

        GridLayout layout = new GridLayout(2, true);
        setLayout(layout);

        Composite listGroup = UIUtils.createPlaceholder(this, 1);
        listGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable = new Table(listGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        homesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = homesTable.getSelection();
                if (CommonUtils.isEmpty(selection)) {
                    selectHome(null);
                } else {
                    selectHome((HomeInfo)selection[0].getData());
                }
            }
        });
        Composite buttonsGroup = UIUtils.createPlaceholder(listGroup, 2);
        buttonsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        Button addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText("Add Home");
        addButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                addClientHome();
            }
        });
        removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText("Remove Home");
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = homesTable.getSelection();
                if (!CommonUtils.isEmpty(selection)) {
                    removeClientHome();
                }
            }
        });

        Group infoGroup = UIUtils.createControlGroup(this, "Information", 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        idText = UIUtils.createLabelText(infoGroup, "ID", "", SWT.BORDER | SWT.READ_ONLY);
        pathText = UIUtils.createLabelText(infoGroup, "Path", "", SWT.BORDER | SWT.READ_ONLY);
        nameText = UIUtils.createLabelText(infoGroup, "Name", "", SWT.BORDER | SWT.READ_ONLY);
        productNameText = UIUtils.createLabelText(infoGroup, "Product Name", "", SWT.BORDER | SWT.READ_ONLY);
        productVersionText = UIUtils.createLabelText(infoGroup, "Product Version", "", SWT.BORDER | SWT.READ_ONLY);
    }

    private void removeClientHome()
    {
        int selIndex = homesTable.getSelectionIndex();
        HomeInfo info = (HomeInfo) homesTable.getItem(selIndex).getData();
        if (!info.isProvided) {
            if (UIUtils.confirmAction(
                getShell(),
                "Remove Client Home",
                "Are you sure you want to delete client home '" + info.home.getHomeId() + "'?"))
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
        DBPClientManager clientManager = driver.getClientManager();
        if (clientManager != null) {
            createHomeItem(clientManager, homeId, false);
        }
    }

    private void selectHome(HomeInfo home)
    {
        removeButton.setEnabled(home != null && !home.isProvided);
        idText.setText(home == null ? "" : CommonUtils.getString(home.home.getHomeId()));
        pathText.setText(home == null ? "" : home.home.getHomePath().getAbsolutePath());
        nameText.setText(home == null ? "" : CommonUtils.getString(home.home.getDisplayName()));
        try {
            productNameText.setText(home == null ? "" : CommonUtils.getString(home.home.getProductName()));
        } catch (DBException e) {
            log.warn(e);
        }
        try {
            productVersionText.setText(home == null ? "" : CommonUtils.getString(home.home.getProductVersion()));
        } catch (DBException e) {
            log.warn(e);
        }
    }

    public Collection<String> getHomeIds()
    {
        java.util.List<String> homes = new ArrayList<String>();
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
        DBPClientManager clientManager = this.driver.getClientManager();
        if (clientManager == null) {
            log.error("Client manager is not supported by driver '" + driver.getName() + "'");
            return;
        }
        Set<String> providedHomes = new LinkedHashSet<String>(
            clientManager.findClientHomeIds());
        Set<String> allHomes = new LinkedHashSet<String>(providedHomes);
        allHomes.addAll(driver.getClientHomeIds());

        for (String homeId : allHomes) {
            createHomeItem(clientManager, homeId, providedHomes.contains(homeId));
        }
    }

    private void createHomeItem(DBPClientManager clientManager, String homeId, boolean provided)
    {
        DBPClientHome home;
        try {
            home = clientManager.getClientHome(homeId);
            if (home == null) {
                log.warn("Home '" + homeId + "' is not supported");
                return;
            }
        } catch (Exception e) {
            log.error(e);
            return;
        }
        HomeInfo homeInfo = new HomeInfo(home);
        homeInfo.isProvided = provided;
        homeInfo.isDefault = home.getHomeId().equals(clientManager.getDefaultClientHomeId());
        TableItem homeItem = new TableItem(homesTable, SWT.NONE);
        homeItem.setText(home.getDisplayName());
        homeItem.setImage(DBIcon.HOME.getImage());
        homeItem.setData(homeInfo);
        if (!homeInfo.isProvided) {
            homeItem.setFont(fontItalic);
        } else {
            if (homeInfo.isDefault) {
                homeItem.setFont(fontBold);
            }
        }
    }

    private String getSelectedHome()
    {
        TableItem[] selection = homesTable.getSelection();
        if (CommonUtils.isEmpty(selection)) {
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

        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Database Client Homes");

            panel = new ClientHomesPanel(parent, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            panel.setLayoutData(gd);
            panel.loadHomes(driver);

            return parent;
        }

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