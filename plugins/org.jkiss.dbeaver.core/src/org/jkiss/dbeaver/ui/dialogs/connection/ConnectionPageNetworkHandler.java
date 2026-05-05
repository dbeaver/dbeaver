/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageProjectNetworkProfiles;
import org.jkiss.utils.CommonUtils;

/**
 * Network handlers edit dialog page
 */
public class ConnectionPageNetworkHandler extends ConnectionWizardPage {

    private static final Log log = Log.getLog(ConnectionPageNetworkHandler.class);

    private final IDataSourceConnectionEditorSite site;
    private final NetworkHandlerDescriptor handlerDescriptor;

    private IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> configurator;
    private Composite configuratorPlaceholder;
    private ControlEnableState configuratorEnableState;

    // Handler configuration. If null, no configuration is saved nor updated.
    private DBWHandlerConfiguration handlerConfiguration;

    // Shown when a handler is provided by a profile
    private Link profileProvidedHint;

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

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        profileProvidedHint = UIUtils.createInfoLink(
            composite,
            "N/A",
            () -> PrefPageProjectNetworkProfiles.open(getShell(), site.getProject(), getActiveProfile())
        );

        configuratorPlaceholder = UIUtils.createComposite(composite, 1);
        configuratorPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(configuratorPlaceholder, handlerDescriptor, this::updatePageCompletion);

        setControl(composite);
        refreshConfiguration(getActiveProfile());
    }

    @Override
    protected void updatePageCompletion() {
        if (isPageComplete()) {
            setPageComplete(true);
            setErrorMessage(null);
        } else {
            setPageComplete(false);
            setErrorMessage(configurator.getErrorMessage());
        }
    }

    @Override
    public boolean isPageComplete() {
        return handlerConfiguration == null || !handlerConfiguration.isEnabled() || configurator.isComplete();
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        if (handlerConfiguration == null) {
            return;
        }
        configuration.setConfigProfile(getActiveProfile());
        if (handlerConfiguration.isEnabled()) {
            configurator.saveSettings(handlerConfiguration);
            configuration.updateHandler(handlerConfiguration);
        } else {
            configuration.removeHandler(handlerConfiguration.getId());
        }
    }

    /**
     * Refreshes the configuration for this page, reverting all modifications made after page is loaded.
     * <p>
     * If a {@code profile} is present, then the page's controls are disabled,
     * and configuration is loaded from that profile.
     * <p>
     * Otherwise, page's controls are enabled, and configuration is loaded
     * from the connection configuration, if present.
     */
    public void refreshConfiguration(@Nullable DBWNetworkProfile profile) {
        loadConfiguration(profile);

        if (handlerConfiguration != null) {
            configurator.loadSettings(handlerConfiguration);
        }
        configuratorPlaceholder.setRedraw(false);

        if (configuratorEnableState != null) {
            configuratorEnableState.restore();
            configuratorEnableState = null;
        }

        DBWHandlerConfiguration profileConfiguration = profile != null ? profile.getConfiguration(handlerDescriptor) : null;

        if (profileConfiguration != null && profileConfiguration.isEnabled()) {
            if (configuratorEnableState == null) {
                configuratorEnableState = ControlEnableState.disable(configuratorPlaceholder);
            }

            profileProvidedHint.setText(NLS.bind(
                "Using configuration from profile ''<a href=\"#\">{0}</a>''",
                profile.getProfileName()
            ));
            UIUtils.setControlVisible(profileProvidedHint.getParent(), true);
        } else {
             UIUtils.setControlVisible(profileProvidedHint.getParent(), false);
        }

        configuratorPlaceholder.getParent().layout(true, true);
        configuratorPlaceholder.setRedraw(true);

        updatePageCompletion();
    }

    public void loadConfiguration(@Nullable DBWNetworkProfile profile) {
        DBWHandlerConfiguration profileConfiguration = profile != null ? profile.getConfiguration(handlerDescriptor) : null;

        if (profile != null) {
            // Use configuration from the profile
            if (profileConfiguration != null && profileConfiguration.isEnabled()) {
                handlerConfiguration = new DBWHandlerConfiguration(profileConfiguration);
            } else {
                log.warn("Attempt to configure " + handlerDescriptor.getId() + " with profile " + profile.getProfileName()
                    + " set that doesn't provide it");
            }
        } else {
            // Use configuration from the connection
            DBPDataSourceContainer dataSource = site.getActiveDataSource();
            DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();

            handlerConfiguration = configuration.getHandler(handlerDescriptor.getId());

            // It could not exist, let's create it
            if (handlerConfiguration == null) {
                handlerConfiguration = new DBWHandlerConfiguration(handlerDescriptor, dataSource);
                configuration.updateHandler(handlerConfiguration);
            }
        }
    }

    /**
     * Returns the handler configuration. It is guaranteed to be {@code null} if the page is not yet loaded.
     */
    @Nullable
    public DBWHandlerConfiguration getHandlerConfiguration() {
        return handlerConfiguration;
    }

    @NotNull
    public NetworkHandlerDescriptor getHandlerDescriptor() {
        return handlerDescriptor;
    }

    @Nullable
    private DBWNetworkProfile getActiveProfile() {
        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        if (CommonUtils.isEmpty(configuration.getConfigProfileName())) {
            return null;
        }
        return dataSource.getRegistry().getNetworkProfile(
            configuration.getConfigProfileSource(),
            configuration.getConfigProfileName()
        );
    }
}
