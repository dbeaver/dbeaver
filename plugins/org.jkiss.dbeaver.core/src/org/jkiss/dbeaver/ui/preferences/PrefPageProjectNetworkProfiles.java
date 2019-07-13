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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomSashForm;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectNetworkProfiles extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.project.settings.networkProfiles"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectNetworkProfiles.class);

    private static class HandlerBlock {
        private final IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        private final Composite blockControl;
        private final Button useHandlerCheck;
        private final TabItem tabItem;
        private ControlEnableState blockEnableState;
        private final Map<DBWNetworkProfile, DBWHandlerConfiguration> loadedConfigs = new HashMap<>();

        private HandlerBlock(IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator, Composite blockControl, Button useHandlerCheck, TabItem tabItem)
        {
            this.configurator = configurator;
            this.blockControl = blockControl;
            this.useHandlerCheck = useHandlerCheck;
            this.tabItem = tabItem;
        }
    }

    private IProject project;
    private DBPProject projectMeta;

    private Table profilesTable;
    private TabFolder handlersFolder;
    private List<NetworkHandlerDescriptor> allHandlers = new ArrayList<>();
    private DBWNetworkProfile selectedProfile;
    private Map<NetworkHandlerDescriptor, HandlerBlock> configurations = new HashMap<>();

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(final Composite parent) {
        CustomSashForm divider = UIUtils.createPartDivider(null, parent, SWT.HORIZONTAL);

        {
            Composite profilesGroup = UIUtils.createComposite(divider, 1);
            GridData gd = new GridData(GridData.FILL_BOTH);
            profilesGroup.setLayoutData(gd);

            profilesTable = new Table(profilesGroup, SWT.BORDER | SWT.SINGLE);
            profilesTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 150;
            profilesTable.setLayoutData(gd);
            profilesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem[] selection = profilesTable.getSelection();
                    if (ArrayUtils.isEmpty(selection)) {
                        selectedProfile = null;
                    } else {
                        selectedProfile = (DBWNetworkProfile) selection[0].getData();
                    }
                    updateControlsState();
                }
            });

            ToolBar toolbar = new ToolBar(profilesGroup, SWT.HORIZONTAL);

            ToolItem createItem = new ToolItem(toolbar, SWT.NONE);
            createItem.setToolTipText("Create new profile");
            createItem.setImage(DBeaverIcons.getImage(UIIcon.ROW_ADD));
            createItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String profileName = EnterNameDialog.chooseName(getShell(), "Profile name", "");
                    if (CommonUtils.isEmpty(profileName)) {
                        return;
                    }
                    DBWNetworkProfile newProfile = new DBWNetworkProfile();
                    newProfile.setProfileId(UUID.randomUUID().toString());
                    newProfile.setProfileName(profileName);
                    projectMeta.getDataSourceRegistry().updateNetworkProfile(newProfile);
                    projectMeta.getDataSourceRegistry().flushConfig();

                    TableItem item = new TableItem(profilesTable, SWT.NONE);
                    item.setText(newProfile.getProfileName());
                    item.setImage(DBeaverIcons.getImage(DBIcon.TYPE_DOCUMENT));
                    item.setData(newProfile);
                }
            });

            ToolItem deleteItem = new ToolItem(toolbar, SWT.NONE);
            deleteItem.setToolTipText("Delete profile");
            deleteItem.setImage(DBeaverIcons.getImage(UIIcon.ROW_DELETE));
            deleteItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (selectedProfile != null) {
                        projectMeta.getDataSourceRegistry().removeNetworkProfile(selectedProfile);
                        projectMeta.getDataSourceRegistry().flushConfig();
                        profilesTable.remove(profilesTable.getSelectionIndex());
                        selectedProfile = null;
                        updateControlsState();
                    } else {
                        UIUtils.showMessageBox(getShell(), "No profile", "Select profile first", SWT.ICON_ERROR);
                    }
                }
            });

        }

        {
            handlersFolder = new TabFolder(divider, SWT.TOP | SWT.FLAT);
            handlersFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
            for (NetworkHandlerDescriptor nhd : NetworkHandlerRegistry.getInstance().getDescriptors()) {
                if (!nhd.hasObjectTypes()) {
                    createHandlerTab(nhd);
                }
            }
            handlersFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateControlsState();
                }
            });
        }

        divider.setWeights(new int[] { 300, 700 } );

        performDefaults();
        updateControlsState();

        return divider;
    }

    private void updateControlsState() {
        TabItem[] selection = handlersFolder.getSelection();
        NetworkHandlerDescriptor descriptor = ArrayUtils.isEmpty(selection) ? null : (NetworkHandlerDescriptor) selection[0].getData();
        enableHandlerContent(descriptor);
    }

    private void createHandlerTab(final NetworkHandlerDescriptor descriptor)
    {
        IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        try {
            String implName = descriptor.getHandlerType().getImplName();
            UIPropertyConfiguratorDescriptor configDescriptor = UIPropertyConfiguratorRegistry.getInstance().getDescriptor(implName);
            if (configDescriptor == null) {
                return;
            }
            configurator = configDescriptor.createConfigurator();
        } catch (DBException e) {
            log.error("Can't create network configurator '" + descriptor.getId() + "'", e);
            return;
        }
        allHandlers.add(descriptor);

        TabItem tabItem = new TabItem(handlersFolder, SWT.NONE);
        tabItem.setText(descriptor.getLabel());
        tabItem.setToolTipText(descriptor.getDescription());
        tabItem.setData(descriptor);

        Composite composite = new Composite(handlersFolder, SWT.NONE);
        tabItem.setControl(composite);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Button useHandlerCheck = UIUtils.createCheckbox(composite, NLS.bind(CoreMessages.dialog_tunnel_checkbox_use_handler, descriptor.getLabel()), false);
        useHandlerCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (selectedProfile == null) {
                    useHandlerCheck.setSelection(false);
                    UIUtils.showMessageBox(getShell(), "No profile", "Select existing profile or create a new one", SWT.ICON_INFORMATION);
                    return;
                }
                HandlerBlock handlerBlock = configurations.get(descriptor);
                DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(selectedProfile);
                if (handlerConfiguration == null) {
                    handlerConfiguration = new DBWHandlerConfiguration(descriptor, null);
                    handlerBlock.loadedConfigs.put(selectedProfile, handlerConfiguration);
                }
                handlerConfiguration.setEnabled(useHandlerCheck.getSelection());
                enableHandlerContent(descriptor);
            }
        });
        Composite handlerComposite = UIUtils.createPlaceholder(composite, 1);
        configurations.put(descriptor, new HandlerBlock(configurator, handlerComposite, useHandlerCheck, tabItem));

        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite);

        enableHandlerContent(descriptor);
    }

    private void enableHandlerContent(NetworkHandlerDescriptor descriptor)
    {
        HandlerBlock handlerBlock = configurations.get(descriptor);
        DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(selectedProfile);
        handlerBlock.useHandlerCheck.setSelection(handlerConfiguration != null && handlerConfiguration.isEnabled());
        if (handlerConfiguration != null && handlerConfiguration.isEnabled()) {
            if (handlerBlock.blockEnableState != null) {
                handlerBlock.blockEnableState.restore();
                handlerBlock.blockEnableState = null;
            }
        } else if (handlerBlock.blockEnableState == null) {
            handlerBlock.blockEnableState = ControlEnableState.disable(handlerBlock.blockControl);
        }
    }

    public void saveSettings(DBWNetworkProfile profile) {
        for (HandlerBlock handlerBlock : configurations.values()) {
            DBWHandlerConfiguration configuration = handlerBlock.loadedConfigs.get(profile);
            if (configuration != null) {
                profile.getConfigurations().add(configuration);
            }
        }
    }


    @Override
    protected void performDefaults() {
        super.performDefaults();

        profilesTable.removeAll();
        if (projectMeta != null) {
            for (DBWNetworkProfile profile : projectMeta.getDataSourceRegistry().getNetworkProfiles()) {

                TableItem item = new TableItem(profilesTable, SWT.NONE);
                item.setText(profile.getProfileName());
                item.setImage(DBeaverIcons.getImage(DBIcon.TYPE_DOCUMENT));
                item.setData(profile);

                for (NetworkHandlerDescriptor nhd : allHandlers) {
                    HandlerBlock handlerBlock = configurations.get(nhd);
                    DBWHandlerConfiguration configuration = profile.getConfiguration(nhd);
                    if (configuration != null) {
                        handlerBlock.loadedConfigs.put(profile, configuration);
                    }
                }
            }
        }

    }

    @Override
    public boolean performOk() {
        for (TableItem item : profilesTable.getItems()) {
            DBWNetworkProfile profile = (DBWNetworkProfile) item.getData();
            saveSettings(profile);
            projectMeta.getDataSourceRegistry().updateNetworkProfile(profile);
        }
        projectMeta.getDataSourceRegistry().flushConfig();

        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return project;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.project = (IProject) element;
        this.projectMeta = DBWorkbench.getPlatform().getWorkspace().getProject(this.project);
    }

}
