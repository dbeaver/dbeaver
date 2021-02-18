/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * ClientHomesSelector
 */
public class ClientHomesSelector implements ISelectionProvider {
    private Composite selectorPanel;
    private Combo homesCombo;
    //private Label versionLabel;
    private DBPDriver driver;
    private List<String> homeIds = new ArrayList<>();
    private String currentHomeId;
    private int currentHomeIndex;

    private final Map<ISelectionChangedListener, SelectionListener> listeners = new IdentityHashMap<>();

    public ClientHomesSelector(
        Composite parent,
        String title)
    {
        this(parent, title, true);
    }
    public ClientHomesSelector(
        Composite parent,
        String title,
        boolean createComposite)
    {
        selectorPanel = createComposite ? UIUtils.createComposite(parent, 2) : parent;

        Label controlLabel = UIUtils.createControlLabel(selectorPanel, title);
        controlLabel.setToolTipText(UIConnectionMessages.controls_client_home_selector_tip);
        //label.setFont(UIUtils.makeBoldFont(label.getFont()));
        homesCombo = new Combo(selectorPanel, SWT.READ_ONLY);
        //directoryDialog = new DirectoryDialog(selectorContainer.getShell(), SWT.OPEN);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = UIUtils.getFontHeight(homesCombo) * 30;
        homesCombo.setLayoutData(gd);
        homesCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (homesCombo.getSelectionIndex() == homesCombo.getItemCount() - 1) {
                    homesCombo.select(currentHomeIndex);
                    manageHomes();
                } else {
                    currentHomeId = homeIds.get(homesCombo.getSelectionIndex());
                    currentHomeIndex = homesCombo.getSelectionIndex();
                }
                displayClientVersion();
                handleHomeChange();
            }
        });
        homesCombo.setEnabled(false);
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
        populateHomes(driver, currentHomeId, true);
    }

    public void populateHomes(DBPDriver driver, String currentHome, boolean selectDefault)
    {
        this.driver = driver;
        this.currentHomeId = currentHome;

        this.homesCombo.removeAll();
        this.homeIds.clear();

        Map<String, DBPNativeClientLocation> homes = new LinkedHashMap<>();

        AbstractJob hlJob = new AbstractJob("Find native client homes") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                for (DBPNativeClientLocation ncl : driver.getNativeClientLocations()) {
                    homes.put(ncl.getName(), ncl);
                }

                DBPNativeClientLocationManager clientManager = driver.getNativeClientManager();
                if (clientManager != null) {
                    for (DBPNativeClientLocation location : clientManager.findLocalClientLocations()) {
                        homes.putIfAbsent(location.getName(), location);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        hlJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                UIUtils.syncExec(() -> {
                    for (DBPNativeClientLocation location : homes.values()) {
                        homesCombo.add(location.getDisplayName());
                        homeIds.add(location.getName());
                        if (currentHomeId != null && location.getName().equals(currentHomeId)) {
                            homesCombo.select(homesCombo.getItemCount() - 1);
                        }
                    }
                    if (homesCombo.getItemCount() == 0) {
                        homesCombo.add(UIConnectionMessages.controls_client_home_selector_missing);
                        homeIds.add(null);
                    }
                    if (selectDefault && homesCombo.getSelectionIndex() == -1) {
                        homesCombo.select(0);
                        currentHomeId = homeIds.get(0);
                    }
                    homesCombo.add(UIConnectionMessages.controls_client_home_selector_browse);

                    displayClientVersion();

                    homesCombo.setEnabled(true);
                });
                super.done(event);
            }
        });
        hlJob.schedule();
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

    @Override
    public ISelection getSelection() {
        int selectionIndex = homesCombo.getSelectionIndex();
        String selection = selectionIndex < 0 ? null : homesCombo.getItem(selectionIndex);
        return selection == null ? new StructuredSelection() : new StructuredSelection(selection);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listener.selectionChanged(new SelectionChangedEvent(ClientHomesSelector.this, getSelection()));
            }
        };
        homesCombo.addSelectionListener(selectionAdapter);
        listeners.put(listener, selectionAdapter);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        homesCombo.removeSelectionListener(listeners.remove(listener));
    }

    @Override
    public void setSelection(ISelection selection) {

    }
}