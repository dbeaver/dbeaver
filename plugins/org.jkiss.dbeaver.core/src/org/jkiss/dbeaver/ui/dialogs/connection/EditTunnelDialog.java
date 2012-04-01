/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;

import java.util.*;

/**
 * Network handlers edit dialog
 */
public class EditTunnelDialog extends HelpEnabledDialog {

    static final Log log = LogFactory.getLog(EditTunnelDialog.class);

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

    protected EditTunnelDialog(Shell shell, DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        super(shell, IHelpContextIds.CTX_EDIT_CONNECTION_TUNNELS);
        this.driver = driver;
        this.connectionInfo = connectionInfo;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Configure connection handlers");
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        TabFolder tunnelTypeFolder = new TabFolder(composite, SWT.TOP);
        tunnelTypeFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        NetworkHandlerRegistry registry = DBeaverCore.getInstance().getNetworkHandlerRegistry();
        for (NetworkHandlerDescriptor descriptor : registry.getDescriptors()) {
            try {
                createHandlerTab(tunnelTypeFolder, descriptor);
            } catch (DBException e) {
                log.warn(e);
            }
        }
        
        return composite;
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

        final Button useHandlerCheck = UIUtils.createCheckbox(composite, "Use " + descriptor.getLabel(), false);
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

    private void saveConfigurations()
    {
        java.util.List<DBWHandlerConfiguration> handlers = new ArrayList<DBWHandlerConfiguration>();
        for (HandlerBlock handlerBlock : configurations.values()) {
            handlerBlock.configurator.saveSettings(handlerBlock.configuration);
            handlers.add(handlerBlock.configuration);
        }
        connectionInfo.setHandlers(handlers);
    }

    @Override
    protected void okPressed()
    {
        saveConfigurations();
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}
