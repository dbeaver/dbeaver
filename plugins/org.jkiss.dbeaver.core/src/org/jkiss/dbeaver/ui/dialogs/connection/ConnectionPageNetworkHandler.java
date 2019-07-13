/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Network handlers edit dialog page
 */
public class ConnectionPageNetworkHandler extends ConnectionWizardPage {

    private static final Log log = Log.getLog(ConnectionPageNetworkHandler.class);

    private final IDataSourceConnectionEditorSite site;
    private final NetworkHandlerDescriptor handlerDescriptor;

    private IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
    private Composite blockControl;
    private Button useHandlerCheck;
    private TabItem tabItem;
    private ControlEnableState blockEnableState;
    private DBWHandlerConfiguration handlerConfiguration;
    private Composite handlerComposite;

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
            handlerConfiguration = new DBWHandlerConfiguration(handlerDescriptor, dataSource.getDriver());
            connectionConfiguration.addHandler(handlerConfiguration);
        }

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Button useHandlerCheck = UIUtils.createCheckbox(composite, NLS.bind(CoreMessages.dialog_tunnel_checkbox_use_handler, handlerDescriptor.getLabel()), false);
        useHandlerCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handlerConfiguration.setEnabled(useHandlerCheck.getSelection());
                enableHandlerContent();
            }
        });
        handlerComposite = UIUtils.createPlaceholder(composite, 1);
        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite);
        configurator.loadSettings(handlerConfiguration);
        useHandlerCheck.setSelection(handlerConfiguration.isEnabled());
        enableHandlerContent();

        setControl(composite);
    }

    protected void enableHandlerContent() {
        if (handlerConfiguration.isEnabled()) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else if (blockEnableState == null) {
            blockEnableState = ControlEnableState.disable(handlerComposite);
        }
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource) {
        configurator.saveSettings(handlerConfiguration);
    }

}
