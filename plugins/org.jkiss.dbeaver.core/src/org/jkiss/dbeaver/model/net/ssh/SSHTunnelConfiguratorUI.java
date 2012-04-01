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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

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
    private Composite pkControlGroup;

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
        portText = UIUtils.createLabelSpinner(composite, "Port", SSHConstants.DEFAULT_SSH_PORT, 0, 65535);
        userNameText = UIUtils.createLabelText(composite, "User Name", "");
        userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        authMethodCombo = UIUtils.createLabelCombo(composite, "Authentication Method", SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        authMethodCombo.setLayoutData(gd);
        authMethodCombo.add("Password");
        authMethodCombo.add("Public Key");

        privateKeyLabel = UIUtils.createControlLabel(composite, "Private Key");
        privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        pkControlGroup = UIUtils.createPlaceholder(composite, 2);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        pkControlGroup.setLayoutData(gd);
        privateKeyText = new Text(pkControlGroup, SWT.BORDER);
        privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button openFolder = new Button(pkControlGroup, SWT.PUSH);
        openFolder.setImage(DBIcon.TREE_FOLDER.getImage());
        openFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fd = new FileDialog(composite.getShell(), SWT.OPEN | SWT.SINGLE);
                fd.setText("Choose private key file");
                String[] filterExt = {"*.*", "*.ssh"}; //$NON-NLS-1$ //$NON-NLS-2$
                fd.setFilterExtensions(filterExt);
                String selected = ContentUtils.openFileDialog(fd);
                if (selected != null) {
                    privateKeyText.setText(selected);
                }
            }
        });

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
        userNameText.setText(CommonUtils.getString(configuration.getUserName()));
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
        Map<String,String> properties = configuration.getProperties();
        properties.clear();
        properties.put(SSHConstants.PROP_HOST, hostText.getText());
        properties.put(SSHConstants.PROP_PORT, portText.getText());
        properties.put(SSHConstants.PROP_AUTH_TYPE,
            authMethodCombo.getSelectionIndex() == 0 ?
                SSHConstants.AuthType.PASSWORD.name() :
                SSHConstants.AuthType.PUBLIC_KEY.name());
        properties.put(SSHConstants.PROP_KEY_PATH, privateKeyText.getText());
        configuration.setUserName(userNameText.getText());
        configuration.setPassword(passwordText.getText());
        configuration.setSavePassword(savePasswordCheckbox.getSelection());
    }

    private void updatePrivateKeyVisibility()
    {
        boolean isPassword = authMethodCombo.getSelectionIndex() == 0;
        GridData gd = (GridData)pkControlGroup.getLayoutData();
        gd.exclude = isPassword;
        gd = (GridData)privateKeyLabel.getLayoutData();
        gd.exclude = isPassword;
        pkControlGroup.setVisible(!isPassword);
        privateKeyLabel.setVisible(!isPassword);
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
