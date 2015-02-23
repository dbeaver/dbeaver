/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPClientHome;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ClientHomesSelector
 */
public class ClientHomesSelector extends Composite
{
    static final Log log = Log.getLog(ClientHomesSelector.class);

    private Combo homesCombo;
    //private Label versionLabel;
    private DBPDriver driver;
    private List<String> homeIds = new ArrayList<String>();
    private String currentHomeId;

    public ClientHomesSelector(
        Composite parent,
        int style,
        String title)
    {
        super(parent, style);

        this.setLayout(new GridLayout(2, false));

        UIUtils.createControlLabel(this, title);
        //label.setFont(UIUtils.makeBoldFont(label.getFont()));
        homesCombo = new Combo(this, SWT.READ_ONLY);
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

    private void manageHomes()
    {
        String newHomeId = ClientHomesPanel.chooseClientHome(getShell(), driver);
        if (newHomeId != null) {
            currentHomeId = newHomeId;
        }
        populateHomes(driver, currentHomeId);
    }

    public void populateHomes(DBPDriver driver, String currentHome)
    {
        this.driver = driver;
        this.currentHomeId = currentHome;

        this.homesCombo.removeAll();
        this.homeIds.clear();

        Set<String> homes = new LinkedHashSet<String>(
            driver.getClientManager().findClientHomeIds());
        homes.addAll(driver.getClientHomeIds());

        for (String homeId : homes) {
            DBPClientHome home = driver.getClientHome(homeId);
            if (home != null) {
                homesCombo.add(home.getDisplayName());
                homeIds.add(home.getHomeId());
                if (currentHomeId != null && home.getHomeId().equals(currentHomeId)) {
                    homesCombo.select(homesCombo.getItemCount() - 1);
                }
            }
        }
        this.homesCombo.add(CoreMessages.controls_client_home_selector_browse);
        displayClientVersion();
    }

    private void displayClientVersion()
    {
/*
        DBPClientHome clientHome = currentHomeId == null ? null : driver.getClientHome(currentHomeId);
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
        return currentHomeId;
    }
}