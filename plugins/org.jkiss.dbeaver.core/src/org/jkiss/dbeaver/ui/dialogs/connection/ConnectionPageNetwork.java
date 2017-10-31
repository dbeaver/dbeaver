/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Network handlers edit dialog page
 */
public class ConnectionPageNetwork extends ActiveWizardPage<ConnectionWizard> {

    public static final String PAGE_NAME = "networkHandlersSettings";

    private static final Log log = Log.getLog(ConnectionPageNetwork.class);
    private TabFolder handlersFolder;
    private DataSourceDescriptor prevDataSource;

    private static class HandlerBlock {
        private final IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        private final Composite blockControl;
        private final Button useHandlerCheck;
        private final TabItem tabItem;
        ControlEnableState blockEnableState;
        private final Map<String, DBWHandlerConfiguration> loadedConfigs = new HashMap<>();

        private HandlerBlock(IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator, Composite blockControl, Button useHandlerCheck, TabItem tabItem)
        {
            this.configurator = configurator;
            this.blockControl = blockControl;
            this.useHandlerCheck = useHandlerCheck;
            this.tabItem = tabItem;
        }
    }

    private final ConnectionWizard wizard;
    private Map<NetworkHandlerDescriptor, HandlerBlock> configurations = new HashMap<>();

    ConnectionPageNetwork(ConnectionWizard wizard)
    {
        super(PAGE_NAME);
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_connection_network_title);
        setDescription(CoreMessages.dialog_tunnel_title);
    }

    @Override
    public void createControl(Composite parent)
    {
        handlersFolder = new TabFolder(parent, SWT.TOP);
        handlersFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        setControl(handlersFolder);
    }

    private void createHandlerTab(final NetworkHandlerDescriptor descriptor) throws DBException
    {
        IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        try {
            configurator = descriptor.createConfigurator();
        } catch (DBException e) {
            log.error("Can't create network configurator '" + descriptor.getId() + "'", e);
            return;
        }

        TabItem tabItem = new TabItem(handlersFolder, SWT.NONE);
        tabItem.setText(descriptor.getLabel());
        tabItem.setToolTipText(descriptor.getDescription());

        Composite composite = new Composite(handlersFolder, SWT.NONE);
        tabItem.setControl(composite);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Button useHandlerCheck = UIUtils.createCheckbox(composite, NLS.bind(CoreMessages.dialog_tunnel_checkbox_use_handler, descriptor.getLabel()), false);
        useHandlerCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                HandlerBlock handlerBlock = configurations.get(descriptor);
                DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(wizard.getPageSettings().getActiveDataSource().getId());
                handlerConfiguration.setEnabled(useHandlerCheck.getSelection());
                enableHandlerContent(descriptor);
            }
        });
        Composite handlerComposite = UIUtils.createPlaceholder(composite, 1);
        configurations.put(descriptor, new HandlerBlock(configurator, handlerComposite, useHandlerCheck, tabItem));

        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite);
    }

    @Override
    public void activatePage() {
        DataSourceDescriptor dataSource = wizard.getPageSettings().getActiveDataSource();
        DriverDescriptor driver = wizard.getSelectedDriver();
        NetworkHandlerRegistry registry = NetworkHandlerRegistry.getInstance();

        if (prevDataSource == null || prevDataSource != dataSource) {
            for (TabItem item : handlersFolder.getItems()) {
                item.dispose();
            }
            for (NetworkHandlerDescriptor descriptor : registry.getDescriptors(dataSource)) {
                try {
                    createHandlerTab(descriptor);
                } catch (DBException e) {
                    log.warn(e);
                }
            }
            prevDataSource = dataSource;
            handlersFolder.layout();
            for (TabItem item : handlersFolder.getItems()) {
                ((Composite)item.getControl()).layout();
            }
        }

        TabItem selectItem = null;
        for (NetworkHandlerDescriptor descriptor : registry.getDescriptors(dataSource)) {
            DBWHandlerConfiguration configuration = dataSource.getConnectionConfiguration().getHandler(descriptor.getId());
            if (configuration == null) {
                configuration = new DBWHandlerConfiguration(descriptor, driver);
            }
            HandlerBlock handlerBlock = configurations.get(descriptor);
            if (handlerBlock == null) {
                continue;
            }
            //handlerBlock.useHandlerCheck.setSelection(configuration.isEnabled());
            if (selectItem == null && configuration.isEnabled()) {
                selectItem = handlerBlock.tabItem;
            }
            if (!handlerBlock.loadedConfigs.containsKey(dataSource.getId())) {
                handlerBlock.configurator.loadSettings(configuration);
                handlerBlock.loadedConfigs.put(dataSource.getId(), configuration);
            }
            enableHandlerContent(descriptor);
        }
        if (selectItem != null) {
            handlersFolder.setSelection(selectItem);
        } else {
            handlersFolder.setSelection(0);
        }
    }

    protected void enableHandlerContent(NetworkHandlerDescriptor descriptor)
    {
        HandlerBlock handlerBlock = configurations.get(descriptor);
        DBWHandlerConfiguration handlerConfiguration = handlerBlock.loadedConfigs.get(wizard.getPageSettings().getActiveDataSource().getId());
        handlerBlock.useHandlerCheck.setSelection(handlerConfiguration.isEnabled());
        if (handlerConfiguration.isEnabled()) {
            if (handlerBlock.blockEnableState != null) {
                handlerBlock.blockEnableState.restore();
                handlerBlock.blockEnableState = null;
            }
        } else if (handlerBlock.blockEnableState == null) {
            handlerBlock.blockEnableState = ControlEnableState.disable(handlerBlock.blockControl);
        }
    }

    void saveConfigurations(DataSourceDescriptor dataSource)
    {
        boolean foundHandlers = false;
        java.util.List<DBWHandlerConfiguration> handlers = new ArrayList<>();
        for (HandlerBlock handlerBlock : configurations.values()) {
            DBWHandlerConfiguration configuration = handlerBlock.loadedConfigs.get(dataSource.getId());
            if (configuration != null) {
                foundHandlers = true;
                handlerBlock.configurator.saveSettings(configuration);
                handlers.add(configuration);
            }
        }
        if (foundHandlers) {
            dataSource.getConnectionConfiguration().setHandlers(handlers);
        }
    }

}
