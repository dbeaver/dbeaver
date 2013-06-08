/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
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
public class EditTunnelDialogPage extends ActiveWizardPage {

    static final Log log = LogFactory.getLog(EditTunnelDialogPage.class);

    private static class HandlerBlock {
        IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator;
        DBWHandlerConfiguration configuration;
        Composite blockControl;
        ControlEnableState blockEnableState;

        private HandlerBlock(IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator, DBWHandlerConfiguration configuration, Composite blockControl)
        {
            this.configurator = configurator;
            this.configuration = configuration;
            this.blockControl = blockControl;
        }
    }

    private DBPDriver driver;
    private DBPConnectionInfo connectionInfo;
    private Map<NetworkHandlerDescriptor, HandlerBlock> configurations = new HashMap<NetworkHandlerDescriptor, HandlerBlock>();

    EditTunnelDialogPage(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        super(CoreMessages.dialog_tunnel_title);
        this.driver = driver;
        this.connectionInfo = connectionInfo;
        setTitle("Tunnelling (SSH)");
        setDescription(CoreMessages.dialog_tunnel_title);
    }

    @Override
    public void createControl(Composite parent)
    {
        TabFolder tunnelTypeFolder = new TabFolder(parent, SWT.TOP);
        tunnelTypeFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        NetworkHandlerRegistry registry = DBeaverCore.getInstance().getNetworkHandlerRegistry();
        for (NetworkHandlerDescriptor descriptor : registry.getDescriptors()) {
            try {
                createHandlerTab(tunnelTypeFolder, descriptor);
            } catch (DBException e) {
                log.warn(e);
            }
        }
        setControl(tunnelTypeFolder);
    }

    private void createHandlerTab(TabFolder tabFolder, final NetworkHandlerDescriptor descriptor) throws DBException
    {
        IObjectPropertyConfigurator<DBWHandlerConfiguration> configurator = descriptor.createConfigurator();
        DBWHandlerConfiguration configuration = connectionInfo.getHandler(descriptor.getType());
        if (configuration == null) {
            configuration = new DBWHandlerConfiguration(descriptor, driver);
        }
        final DBWHandlerConfiguration config = configuration;
        
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
                config.setEnabled(useHandlerCheck.getSelection());
                enableHandlerContent(descriptor);
            }
        });
        Composite handlerComposite = UIUtils.createPlaceholder(composite, 1);
        configurations.put(descriptor, new HandlerBlock(configurator, configuration, handlerComposite));

        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite);

        useHandlerCheck.setSelection(configuration.isEnabled());

        enableHandlerContent(descriptor);

        configurator.loadSettings(configuration);
    }

    protected void enableHandlerContent(NetworkHandlerDescriptor descriptor)
    {
        HandlerBlock handlerBlock = configurations.get(descriptor);
        if (handlerBlock.configuration.isEnabled()) {
            if (handlerBlock.blockEnableState != null) {
                handlerBlock.blockEnableState.restore();
                handlerBlock.blockEnableState = null;
            }
        } else if (handlerBlock.blockEnableState == null) {
            handlerBlock.blockEnableState = ControlEnableState.disable(handlerBlock.blockControl);
        }
    }

    void saveConfigurations()
    {
        java.util.List<DBWHandlerConfiguration> handlers = new ArrayList<DBWHandlerConfiguration>();
        for (HandlerBlock handlerBlock : configurations.values()) {
            handlerBlock.configurator.saveSettings(handlerBlock.configuration);
            handlers.add(handlerBlock.configuration);
        }
        connectionInfo.setHandlers(handlers);
    }

}
