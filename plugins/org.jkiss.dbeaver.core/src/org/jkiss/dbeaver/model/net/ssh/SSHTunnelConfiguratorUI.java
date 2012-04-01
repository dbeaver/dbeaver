/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.net.ssh;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private Text hostText;
    private Spinner portText;
    private Text userNameText;
    private Combo authMethodCombo;
    private Text privateKeyText;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Label privateKeyLabel;

    @Override
    public void createControl(Composite parent)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);
        composite.setLayout(new GridLayout(2, false));
        hostText = UIUtils.createLabelText(composite, "Host/IP", "");
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        portText = UIUtils.createLabelSpinner(composite, "Port", 22, 0, 65535);
        userNameText = UIUtils.createLabelText(composite, "User Name", "");
        userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        authMethodCombo = UIUtils.createLabelCombo(composite, "Authentication Method", SWT.DROP_DOWN | SWT.READ_ONLY);
        authMethodCombo.add("Password");
        authMethodCombo.add("Public Key");
        privateKeyLabel = UIUtils.createControlLabel(composite, "Private Key");
        privateKeyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        privateKeyText = new Text(composite, SWT.BORDER);
        privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        passwordText = UIUtils.createLabelText(composite, "Password", "", SWT.BORDER | SWT.PASSWORD);

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updatePrivateKeyVisibility();
                composite.layout();
            }
        });
        
        UIUtils.createPlaceholder(composite,1);
        savePasswordCheckbox = UIUtils.createCheckbox(composite, "Save Password", false);
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration)
    {
        hostText.setText(CommonUtils.getString(configuration.getProperties().get(SSHConstants.PROP_HOST)));
        String portString = configuration.getProperties().get(SSHConstants.PROP_PORT);
        if (!CommonUtils.isEmpty(portString)) {
            portText.setSelection(CommonUtils.toInt(portString));
        }
        userNameText.setText(CommonUtils.getString(configuration.getProperties().get(SSHConstants.PROP_USER_NAME)));
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        String authTypeName = configuration.getProperties().get(SSHConstants.PROP_AUTH_TYPE);
        if (!CommonUtils.isEmpty(authTypeName)) {
            authType = SSHConstants.AuthType.valueOf(authTypeName);
        }
        authMethodCombo.select(authType == SSHConstants.AuthType.PASSWORD ? 0 : 1);
        privateKeyText.setText(CommonUtils.getString(configuration.getProperties().get(SSHConstants.PROP_KEY_PATH)));
        passwordText.setText(CommonUtils.getString(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());

        updatePrivateKeyVisibility();
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {

    }

    private void updatePrivateKeyVisibility()
    {
        boolean isPassword = authMethodCombo.getSelectionIndex() == 0;
        GridData gd = (GridData)privateKeyText.getLayoutData();
        gd.exclude = isPassword;
        gd = (GridData)privateKeyLabel.getLayoutData();
        gd.exclude = isPassword;
        privateKeyText.setVisible(!isPassword);
        privateKeyLabel.setVisible(!isPassword);
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
