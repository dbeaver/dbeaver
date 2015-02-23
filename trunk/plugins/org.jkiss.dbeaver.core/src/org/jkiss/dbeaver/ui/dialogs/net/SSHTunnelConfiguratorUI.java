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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.impl.net.SSHConstants;
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
    private Label pwdLabel;
    private Composite pwdControlGroup;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Label privateKeyLabel;
    private Composite pkControlGroup;
    private Spinner keepAliveText;

    @Override
    public void createControl(Composite parent)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);
        composite.setLayout(new GridLayout(2, false));
        hostText = UIUtils.createLabelText(composite, CoreMessages.model_ssh_configurator_label_host_ip, null); //$NON-NLS-2$
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        portText = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_port, SSHConstants.DEFAULT_SSH_PORT, 0, 65535);
        userNameText = UIUtils.createLabelText(composite, CoreMessages.model_ssh_configurator_label_user_name, null); //$NON-NLS-2$
        userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        authMethodCombo = UIUtils.createLabelCombo(composite, CoreMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        authMethodCombo.setLayoutData(gd);
        authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_password);
        authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_pub_key);

        privateKeyLabel = UIUtils.createControlLabel(composite, CoreMessages.model_ssh_configurator_label_private_key);
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
                fd.setText(CoreMessages.model_ssh_configurator_dialog_choose_private_key);
                String[] filterExt = {"*.*", "*.ssh"}; //$NON-NLS-1$ //$NON-NLS-2$
                fd.setFilterExtensions(filterExt);
                String selected = ContentUtils.openFileDialog(fd);
                if (selected != null) {
                    privateKeyText.setText(selected);
                }
            }
        });

        pwdLabel = UIUtils.createControlLabel(composite, CoreMessages.model_ssh_configurator_label_password);
        pwdControlGroup = UIUtils.createPlaceholder(composite, 3);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 200;
        pwdControlGroup.setLayoutData(gd);

        passwordText = new Text(pwdControlGroup, SWT.BORDER | SWT.PASSWORD);

        new Label(pwdControlGroup, SWT.NONE).setText("   ");
        savePasswordCheckbox = UIUtils.createCheckbox(pwdControlGroup, CoreMessages.model_ssh_configurator_checkbox_save_pass, false);

        keepAliveText = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updatePrivateKeyVisibility();
                composite.layout();
            }
        });

    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration)
    {
        hostText.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSHConstants.PROP_HOST)));
        String portString = configuration.getProperties().get(SSHConstants.PROP_PORT);
        if (!CommonUtils.isEmpty(portString)) {
            portText.setSelection(CommonUtils.toInt(portString));
        }
        userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        String authTypeName = configuration.getProperties().get(SSHConstants.PROP_AUTH_TYPE);
        if (!CommonUtils.isEmpty(authTypeName)) {
            authType = SSHConstants.AuthType.valueOf(authTypeName);
        }
        authMethodCombo.select(authType == SSHConstants.AuthType.PASSWORD ? 0 : 1);
        privateKeyText.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSHConstants.PROP_KEY_PATH)));
        passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());

        String kaString = configuration.getProperties().get(SSHConstants.PROP_ALIVE_INTERVAL);
        if (!CommonUtils.isEmpty(kaString)) {
            keepAliveText.setSelection(Integer.parseInt(kaString));
        }

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
        int kaInterval = keepAliveText.getSelection();
        if (kaInterval <= 0) {
            properties.remove(SSHConstants.PROP_ALIVE_INTERVAL);
        } else {
            properties.put(SSHConstants.PROP_ALIVE_INTERVAL, String.valueOf(kaInterval));
        }
    }

    private void updatePrivateKeyVisibility()
    {
        boolean isPassword = authMethodCombo.getSelectionIndex() == 0;
        ((GridData)pkControlGroup.getLayoutData()).exclude = isPassword;
        pkControlGroup.setVisible(!isPassword);
        ((GridData)privateKeyLabel.getLayoutData()).exclude = isPassword;
        privateKeyLabel.setVisible(!isPassword);

//        pwdControlGroup.setVisible(isPassword);
//        ((GridData)pwdControlGroup.getLayoutData()).exclude = !isPassword;
//        pwdLabel.setVisible(isPassword);
//        ((GridData)pwdLabel.getLayoutData()).exclude = !isPassword;
//
//        if (!isPassword) {
//            savePasswordCheckbox.setSelection(true);
//        }
        pwdLabel.setText(isPassword ? CoreMessages.model_ssh_configurator_label_password : CoreMessages.model_ssh_configurator_label_passphrase);
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
