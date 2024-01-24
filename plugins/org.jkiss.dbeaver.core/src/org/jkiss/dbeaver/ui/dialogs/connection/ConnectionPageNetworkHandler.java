/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.DBPDataSourceOriginExternal;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.preferences.PrefPageProjectNetworkProfiles;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Network handlers edit dialog page
 */
public class ConnectionPageNetworkHandler extends ConnectionWizardPage implements IPropertyChangeListener {

    private static final Log log = Log.getLog(ConnectionPageNetworkHandler.class);

    private static final String PROP_CONFIG_PROFILE = "configProfile";

    private final IDataSourceConnectionEditorSite site;
    private final NetworkHandlerDescriptor handlerDescriptor;

    private IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> configurator;
    private ControlEnableState blockEnableState;
    private DBWHandlerConfiguration handlerConfiguration;
    private Composite handlerComposite;
    private Combo profileCombo;
    private Button useHandlerCheck;
    private DBWNetworkProfile activeProfile;
    private final List<DBWNetworkProfile> allProfiles = new ArrayList<>();;

    public ConnectionPageNetworkHandler(IDataSourceConnectionEditorSite site, NetworkHandlerDescriptor descriptor) {
        super(ConnectionPageNetworkHandler.class.getSimpleName() + "." + descriptor.getId());
        this.site = site;
        this.handlerDescriptor = descriptor;

        setTitle(descriptor.getCodeName());
        setDescription(descriptor.getDescription());

    }

    @Override
    public void createControl(Composite parent) {
        try {
            String implName = handlerDescriptor.getHandlerType().getImplName();
            UIPropertyConfiguratorDescriptor configDescriptor = UIPropertyConfiguratorRegistry.getInstance().getDescriptor(implName);
            if (configDescriptor == null) {
                return;
            }
            configurator = configDescriptor.createConfigurator();
        } catch (DBException e) {
            log.error("Can't create network configurator '" + handlerDescriptor.getId() + "'", e);
            return;
        }
        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionConfiguration = dataSource.getConnectionConfiguration();
        handlerConfiguration = connectionConfiguration.getHandler(handlerDescriptor.getId());

        if (handlerConfiguration == null) {
            handlerConfiguration = new DBWHandlerConfiguration(handlerDescriptor, dataSource);
            connectionConfiguration.updateHandler(handlerConfiguration);
        }

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite buttonsGroup = UIUtils.createComposite(composite, 5);
        buttonsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (handlerDescriptor.isPinned()) {
            useHandlerCheck = UIUtils.createCheckbox(buttonsGroup,
                NLS.bind(UIConnectionMessages.dialog_tunnel_checkbox_use_handler, handlerDescriptor.getLabel()), false);
            useHandlerCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handlerConfiguration.setEnabled(useHandlerCheck.getSelection());
                    enableHandlerContent();
                }
            });
        }

        UIUtils.createEmptyLabel(buttonsGroup, 1, 1).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        profileCombo = UIUtils.createLabelCombo(buttonsGroup, "Profile", SWT.READ_ONLY | SWT.DROP_DOWN);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 200;
        profileCombo.setLayoutData(gd);
        profileCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int pi = profileCombo.getSelectionIndex();
                DBWNetworkProfile profile = pi <= 0 ? null : allProfiles.get(pi - 1);
                setConnectionConfigProfile(profile);
            }
        });
        ToolBar editToolbar = new ToolBar(buttonsGroup, SWT.HORIZONTAL);
        ToolItem editItem = new ToolItem(editToolbar, SWT.PUSH);
        editItem.setImage(DBeaverIcons.getImage(UIIcon.EDIT));
        editItem.setToolTipText("Edit profiles");
        editItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog preferenceDialog = PreferencesUtil.createPropertyDialogOn(
                    getShell(),
                    site.getProject().getEclipseProject(),
                    PrefPageProjectNetworkProfiles.PAGE_ID,
                    null,
                    CommonUtils.isEmpty(profileCombo.getText()) ? null : profileCombo.getText());
                if (preferenceDialog != null) {
                    if (preferenceDialog.open() == IDialogConstants.OK_ID) {
                        int pi = profileCombo.getSelectionIndex();
                        setConnectionConfigProfile(pi <= 0 ? null : allProfiles.get(pi - 1));
                    }
                }
            }
        });

        handlerComposite = UIUtils.createComposite(composite, 1);
        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite, handlerDescriptor, this::updatePageCompletion);

        configurator.loadSettings(handlerConfiguration);

        if (useHandlerCheck != null) {
            useHandlerCheck.setSelection(handlerConfiguration.isEnabled());
        }

        enableHandlerContent();
        updateProfileList();

        if (activeProfile != null) {
            DBWHandlerConfiguration profileConfig = activeProfile.getConfiguration(handlerDescriptor);
            if (profileConfig != null) {
                configurator.loadSettings(profileConfig);
            }
        }

        setControl(composite);
    }

    private void setConnectionConfigProfile(DBWNetworkProfile profile) {
        activeProfile = profile;
        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration cfg = dataSource.getConnectionConfiguration();
        String oldProfileId = cfg.getConfigProfileName();
        saveSettings(site.getActiveDataSource());

        if (activeProfile != null) {
            cfg.setConfigProfile(activeProfile);
            handlerConfiguration = cfg.getHandler(handlerDescriptor.getId());
            if (handlerConfiguration == null) {
                handlerConfiguration = new DBWHandlerConfiguration(handlerDescriptor, dataSource);
            }
        } else {
            cfg.setConfigProfile(null);
        }
        site.firePropertyChange(this, PROP_CONFIG_PROFILE, oldProfileId, activeProfile == null ? null : activeProfile.getProfileName());
    }

    private void updateProfileList() {
        DBPConnectionConfiguration cfg = site.getActiveDataSource().getConnectionConfiguration();
        activeProfile = CommonUtils.isEmpty(cfg.getConfigProfileName()) ? null : site.getProject().getDataSourceRegistry().getNetworkProfile(
            cfg.getConfigProfileSource(),
            cfg.getConfigProfileName());

        // Refresh profile list
        profileCombo.removeAll();
        profileCombo.add("");

        allProfiles.clear();
        DBPDataSourceOrigin dataSourceOrigin = site.getActiveDataSource().getOrigin();
        if (dataSourceOrigin instanceof DBPDataSourceOriginExternal) {
            allProfiles.addAll(((DBPDataSourceOriginExternal) dataSourceOrigin).getAvailableNetworkProfiles());
        }
        allProfiles.addAll(site.getProject().getDataSourceRegistry().getNetworkProfiles());
        for (DBWNetworkProfile profile : allProfiles) {
            String profileDisplayName = profile.getProfileName();
            if (profile.isExternallyProvided()) {
                profileDisplayName += " - " + dataSourceOrigin.getDisplayName();
            }
            profileCombo.add(profileDisplayName);
            if (CommonUtils.equalObjects(cfg.getConfigProfileName(), profile.getProfileName()) &&
                CommonUtils.equalObjects(cfg.getConfigProfileSource(), profile.getProfileSource())) {
                profileCombo.select(profileCombo.getItemCount() - 1);
            }
        }

        // Update settings from profile
        if (activeProfile != null) {

        }

        // Update page controls
        handlerConfiguration = cfg.getHandler(handlerDescriptor.getId());
        if (handlerConfiguration == null) {
            handlerConfiguration = new DBWHandlerConfiguration(handlerDescriptor, site.getActiveDataSource());
        }
        if (useHandlerCheck != null) {
            useHandlerCheck.setSelection(handlerConfiguration.isEnabled());
        }
        configurator.loadSettings(handlerConfiguration);
        enableHandlerContent();
    }

    protected void enableHandlerContent() {

        DBWHandlerConfiguration profileConfig = activeProfile == null ? null : activeProfile.getConfiguration(handlerDescriptor);
        boolean hasProfileConfig = profileConfig != null && profileConfig.isEnabled();
        if (handlerConfiguration.isEnabled() && !hasProfileConfig) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else if (blockEnableState == null) {
            blockEnableState = ControlEnableState.disable(handlerComposite);
        }
        if (useHandlerCheck != null) {
            useHandlerCheck.setEnabled(!hasProfileConfig);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {

        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (handlerConfiguration != null) {
            handlerConfiguration.setProperties(Collections.emptyMap());
            configurator.saveSettings(handlerConfiguration);
            dataSource.getConnectionConfiguration().setConfigProfile(activeProfile);
            dataSource.getConnectionConfiguration().updateHandler(handlerConfiguration);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PROP_CONFIG_PROFILE.equals(event.getProperty())) {
            updateProfileList();
        }
    }

    @NotNull
    public NetworkHandlerDescriptor getHandlerDescriptor() {
        return handlerDescriptor;
    }
}
