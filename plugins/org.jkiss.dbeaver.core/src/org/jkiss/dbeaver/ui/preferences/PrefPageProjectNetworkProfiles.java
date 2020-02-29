/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
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
import org.jkiss.dbeaver.utils.GeneralUtils;
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
        private ControlEnableState blockEnableState;
        private final Map<DBWNetworkProfile, DBWHandlerConfiguration> loadedConfigs = new HashMap<>();

        private HandlerBlock(IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator, Composite blockControl, Button useHandlerCheck)
        {
            this.configurator = configurator;
            this.blockControl = blockControl;
            this.useHandlerCheck = useHandlerCheck;
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
            Composite profilesGroup = new Composite(divider, SWT.BORDER);
            GridLayout gl = new GridLayout(1, false);
            gl.marginWidth = 0;
            gl.marginHeight = 0;
            profilesGroup.setLayout(gl);

            GridData gd = new GridData(GridData.FILL_BOTH);
            profilesGroup.setLayoutData(gd);

            {
                ToolBar toolbar = new ToolBar(profilesGroup, SWT.HORIZONTAL);

                UIUtils.createToolItem(toolbar, "Create new profile", UIIcon.ROW_ADD, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String profileName = "";
                        while (true) {
                            profileName = EnterNameDialog.chooseName(getShell(), "Profile name", profileName);
                            if (CommonUtils.isEmptyTrimmed(profileName)) {
                                return;
                            }
                            if (projectMeta.getDataSourceRegistry().getNetworkProfile(profileName) != null) {
                                UIUtils.showMessageBox(getShell(), "Wrong profile name", "Profile '" + profileName + "' already exist in project '" + projectMeta.getName() + "'", SWT.ICON_ERROR);
                                continue;
                            }
                            break;
                        }
                        DBWNetworkProfile newProfile = new DBWNetworkProfile();
                        newProfile.setProfileName(profileName);
                        projectMeta.getDataSourceRegistry().updateNetworkProfile(newProfile);
                        projectMeta.getDataSourceRegistry().flushConfig();

                        TableItem item = new TableItem(profilesTable, SWT.NONE);
                        item.setText(newProfile.getProfileName());
                        item.setImage(DBeaverIcons.getImage(DBIcon.TYPE_DOCUMENT));
                        item.setData(newProfile);
                        if (profilesTable.getItemCount() == 1) {
                            selectedProfile = newProfile;
                            profilesTable.select(0);
                            updateControlsState();
                        }
                    }
                });

                UIUtils.createToolItem(toolbar, "Delete profile", UIIcon.ROW_DELETE, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (selectedProfile != null) {
                            List<? extends DBPDataSourceContainer> usedBy = projectMeta.getDataSourceRegistry().getDataSourcesByProfile(selectedProfile);
                            if (!usedBy.isEmpty()) {
                                UIUtils.showMessageBox(getShell(), "Can't delete profile", "Configuration profile '" + selectedProfile.getProfileName() + "' used by " + usedBy.size() + " connections:\n" + usedBy, SWT.ICON_ERROR);
                                return;
                            }
                            if (!UIUtils.confirmAction(getShell(), "Delete profile", "Are you sure you want to delete configuration profile '" + selectedProfile.getProfileName() + "'?")) {
                                return;
                            }

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

            profilesTable = new Table(profilesGroup, SWT.SINGLE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 150;
            profilesTable.setLayoutData(gd);
            profilesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveHandlerSettings();
                    TableItem[] selection = profilesTable.getSelection();
                    if (ArrayUtils.isEmpty(selection)) {
                        selectedProfile = null;
                    } else {
                        selectedProfile = (DBWNetworkProfile) selection[0].getData();
                    }
                    updateControlsState();
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

        return divider;
    }

    /**
     * Saves state of UI controls to handler configuration
     */
    private void saveHandlerSettings() {
        if (selectedProfile == null) {
            return;
        }
        for (TabItem handlerTab : handlersFolder.getItems()) {
            NetworkHandlerDescriptor handler = (NetworkHandlerDescriptor) handlerTab.getData();
            HandlerBlock handlerBlock = configurations.get(handler);
            DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(selectedProfile);
            if (handlerBlock.useHandlerCheck.getSelection()) {
                if (handlerConfiguration == null) {
                    handlerConfiguration = new DBWHandlerConfiguration(handler, null);
                }
                handlerConfiguration.setProperties(Collections.emptyMap());
                handlerBlock.configurator.saveSettings(handlerConfiguration);
            }
        }
    }

    private void updateControlsState() {
        NetworkHandlerDescriptor descriptor = getSelectedHandler();
        enableHandlerContent(descriptor);

        if (descriptor != null) {
            HandlerBlock handlerBlock = configurations.get(descriptor);
            DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(selectedProfile);
            if (handlerConfiguration == null) {
                handlerBlock.configurator.loadSettings(new DBWHandlerConfiguration(descriptor, null));
            } else {
                handlerBlock.configurator.loadSettings(handlerConfiguration);
            }
        }
    }

    @Nullable
    private NetworkHandlerDescriptor getSelectedHandler() {
        TabItem[] selection = handlersFolder.getSelection();
        return ArrayUtils.isEmpty(selection) ? null : (NetworkHandlerDescriptor) selection[0].getData();
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
        configurations.put(descriptor, new HandlerBlock(configurator, handlerComposite, useHandlerCheck));

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
                profile.updateConfiguration(configuration);
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
                if (selectedProfile == null) {
                    selectedProfile = profile;
                    profilesTable.select(0);
                }

                for (NetworkHandlerDescriptor nhd : allHandlers) {
                    HandlerBlock handlerBlock = configurations.get(nhd);
                    DBWHandlerConfiguration configuration = profile.getConfiguration(nhd);
                    if (configuration != null) {
                        handlerBlock.loadedConfigs.put(profile, configuration);
                    }
                }
            }
        }
        updateControlsState();
    }

    @Override
    public boolean performOk() {
        saveHandlerSettings();

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
        this.project = GeneralUtils.adapt(element, IProject.class);
        this.projectMeta = DBWorkbench.getPlatform().getWorkspace().getProject(this.project);
    }

}
