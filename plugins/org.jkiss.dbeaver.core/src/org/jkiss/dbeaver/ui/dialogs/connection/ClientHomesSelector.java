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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ClientHomesSelector
 */
public class ClientHomesSelector
{
    private Composite selectorPanel;
    private Combo homesCombo;
    //private Label versionLabel;
    private DBPDriver driver;
    private List<String> homeIds = new ArrayList<>();
    private String currentHomeId;

    public ClientHomesSelector(
        Composite parent,
        int style,
        String title)
    {
        selectorPanel = new Composite(parent, style);

        selectorPanel.setLayout(new GridLayout(2, false));

        Label controlLabel = UIUtils.createControlLabel(selectorPanel, title);
        controlLabel.setToolTipText("Local client configuration is needed for some administrative tasks like database dump/restore.");
        //label.setFont(UIUtils.makeBoldFont(label.getFont()));
        homesCombo = new Combo(selectorPanel, SWT.READ_ONLY);
        //directoryDialog = new DirectoryDialog(selectorContainer.getShell(), SWT.OPEN);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        homesCombo.setLayoutData(gd);
        homesCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (homesCombo.getSelectionIndex() == homesCombo.getItemCount() - 1) {
                    manageHomes();
                } else {
                    currentHomeId = homeIds.get(homesCombo.getSelectionIndex());
                }
                displayClientVersion();
                handleHomeChange();
            }
        });
//        versionLabel = new Label(this, SWT.CENTER);
//        gd = new GridData();
//        gd.widthHint = 60;
//        versionLabel.setLayoutData(gd);
    }

    public Composite getPanel() {
        return selectorPanel;
    }

    private void manageHomes()
    {
        String newHomeId = ClientHomesPanel.chooseClientHome(selectorPanel.getShell(), driver);
        if (newHomeId != null) {
            currentHomeId = newHomeId;
        }
        populateHomes(driver, currentHomeId, false);
    }

    public void populateHomes(DBPDriver driver, String currentHome, boolean selectDefault)
    {
        this.driver = driver;
        this.currentHomeId = currentHome;

        this.homesCombo.removeAll();
        this.homeIds.clear();

        Set<DBPNativeClientLocation> homes = new LinkedHashSet<>();
        homes.addAll(driver.getNativeClientLocations());

        DBPNativeClientLocationManager clientManager = driver.getNativeClientManager();
        if (clientManager != null) {
            for (DBPNativeClientLocation location : clientManager.findLocalClientLocations()) {
                if (!homes.contains(location)) {
                    homes.add(location);
                }
            }
        }

        this.homesCombo.add("");
        this.homeIds.add(null);
        for (DBPNativeClientLocation location : homes) {
            homesCombo.add(location.getDisplayName());
            homeIds.add(location.getName());
            if (currentHomeId != null && location.getName().equals(currentHomeId)) {
                homesCombo.select(homesCombo.getItemCount() - 1);
            }
        }
        if (selectDefault && homesCombo.getItemCount() > 1 && homesCombo.getSelectionIndex() == -1) {
            // Select first
            homesCombo.select(1);
        }
        this.homesCombo.add(CoreMessages.controls_client_home_selector_browse);

        displayClientVersion();
    }

    private void displayClientVersion()
    {
/*
        DBPNativeClientLocation clientHome = currentHomeId == null ? null : driver.getNativeClientHome(currentHomeId);
        if (clientHome != null) {
            try {
                // display client version
                if (clientHome.getProductVersion() != null) {
                    versionLabel.setText(clientHome.getProductVersion());
                } else {
                    versionLabel.setText(clientHome.getProductName());
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else {
            versionLabel.setText(""); //$NON-NLS-1$
        }
*/
    }

    protected void handleHomeChange()
    {

    }

    public String getSelectedHome()
    {
        return CommonUtils.isEmpty(currentHomeId) ? null : currentHomeId;
    }

}