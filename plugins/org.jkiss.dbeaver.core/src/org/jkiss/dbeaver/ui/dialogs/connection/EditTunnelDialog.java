/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Network handlers edit dialog
 */
public class EditTunnelDialog extends HelpEnabledDialog {

    static final Log log = LogFactory.getLog(EditTunnelDialog.class);

    private DBPDriver driver;
    private DBPConnectionInfo connectionInfo;
    private Map<NetworkHandlerDescriptor, DBWHandlerConfiguration> configurations = new HashMap<NetworkHandlerDescriptor, DBWHandlerConfiguration>();

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

    private void createHandlerTab(TabFolder tabFolder, NetworkHandlerDescriptor descriptor) throws DBException
    {
        IObjectPropertyConfigurator configurator = descriptor.createConfigurator();

        TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
        tabItem.setText(descriptor.getLabel());
        tabItem.setToolTipText(descriptor.getDescription());

        Composite composite = new Composite(tabFolder, SWT.NONE);
        tabItem.setControl(composite);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        UIUtils.createCheckbox(composite, "Use " + descriptor.getLabel(), false);
        Composite handlerComposite = UIUtils.createPlaceholder(composite, 1);
        handlerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        configurator.createControl(handlerComposite);
        DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(descriptor, driver);
        configurations.put(descriptor, configuration);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}
