/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Network handlers edit dialog page
 */
public class ConnectionPageNetwork extends ActiveWizardPage<ConnectionWizard> {

    static final Log log = Log.getLog(ConnectionPageNetwork.class);
    private TabFolder handlersFolder;

    private static class HandlerBlock {
        private final IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        private final Composite blockControl;
        private final Button useHandlerCheck;
        private final TabItem tabItem;
        ControlEnableState blockEnableState;
        private final Map<String, DBWHandlerConfiguration> loadedConfigs = new HashMap<String, DBWHandlerConfiguration>();

        private HandlerBlock(IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator, Composite blockControl, Button useHandlerCheck, TabItem tabItem)
        {
            this.configurator = configurator;
            this.blockControl = blockControl;
            this.useHandlerCheck = useHandlerCheck;
            this.tabItem = tabItem;
        }
    }

    private final ConnectionWizard wizard;
    private Map<NetworkHandlerDescriptor, HandlerBlock> configurations = new HashMap<NetworkHandlerDescriptor, HandlerBlock>();

    ConnectionPageNetwork(ConnectionWizard wizard)
    {
        super(CoreMessages.dialog_tunnel_title);
        this.wizard = wizard;
        setTitle("Network");
        setDescription(CoreMessages.dialog_tunnel_title);
    }

    @Override
    public void createControl(Composite parent)
    {
        handlersFolder = new TabFolder(parent, SWT.TOP);
        handlersFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        NetworkHandlerRegistry registry = NetworkHandlerRegistry.getInstance();
        for (NetworkHandlerDescriptor descriptor : registry.getDescriptors()) {
            try {
                createHandlerTab(handlersFolder, descriptor);
            } catch (DBException e) {
                log.warn(e);
            }
        }
        setControl(handlersFolder);
    }

    private void createHandlerTab(TabFolder tabFolder, final NetworkHandlerDescriptor descriptor) throws DBException
    {
        IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator = descriptor.createConfigurator();

        TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(descriptor.getLabel());
        tabItem.setToolTipText(descriptor.getDescription());

        Composite composite = new Composite(tabFolder, SWT.NONE);
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
        TabItem selectItem = null;
        for (NetworkHandlerDescriptor descriptor : registry.getDescriptors()) {
            DBWHandlerConfiguration configuration = dataSource.getConnectionInfo().getHandler(descriptor.getId());
            if (configuration == null) {
                configuration = new DBWHandlerConfiguration(descriptor, driver);
            }
            HandlerBlock handlerBlock = configurations.get(descriptor);
            handlerBlock.useHandlerCheck.setSelection(configuration.isEnabled());
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
        java.util.List<DBWHandlerConfiguration> handlers = new ArrayList<DBWHandlerConfiguration>();
        for (HandlerBlock handlerBlock : configurations.values()) {
            DBWHandlerConfiguration configuration = handlerBlock.loadedConfigs.get(dataSource.getId());
            if (configuration != null) {
                foundHandlers = true;
                handlerBlock.configurator.saveSettings(configuration);
                handlers.add(configuration);
            }
        }
        if (foundHandlers) {
            dataSource.getConnectionInfo().setHandlers(handlers);
        }
    }

}
