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
package org.jkiss.dbeaver.ext.dsql.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.dsql.constants.DSQLConstants;
import org.jkiss.dbeaver.ext.dsql.ui.internal.DSQLMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class DSQLAuthIAMConfigurator implements IObjectPropertyConfigurator<Object, DBPDataSourceContainer> {

    @Nullable
    protected Button typeCredentialsRadio;
    @Nullable
    protected Button typeProfileRadio;
    @Nullable
    protected Button typeTokenRadio;

    protected SelectionListener credentialTypeSwitcher;

    // Username
    protected Label usernameLabel;
    protected Text usernameText;

    // AWS Region
    protected Label regionLabel;
    protected Combo regionCombo;

    // AWS Session Credentials
    protected Label accessKeyLabel;
    protected Text accessKeyText;
    protected Label secretKeyLabel;
    protected Text secretKeyText;
    protected Label sessionTokenLabel;
    protected Text sessionTokenText;
    protected List<Control> sessionCredentialWidgets;

    // AWS Profile
    protected Label profileLabel;
    protected Text profileText;
    protected List<Control> profileCredentialWidgets;

    // DSQL Token
    protected Label tokenLabel;
    protected Text tokenText;
    protected List<Control> tokenCredentialWidgets;

    protected DBPDataSourceContainer dataSource;

    @Override
    public void createControl(Composite parent, Object object, Runnable propertyChangeListener) {
        credentialTypeSwitcher = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);

                // Show or hide auth fields
                hideWidgets(sessionCredentialWidgets, typeCredentialsRadio.getSelection());
                hideWidgets(profileCredentialWidgets, typeProfileRadio.getSelection());
                hideWidgets(tokenCredentialWidgets, typeTokenRadio.getSelection());

                regionCombo.setEnabled(!typeTokenRadio.getSelection());

                // refresh the entire auth box
                parent.getParent().getParent().layout(true, true);
            }
        };

        usernameLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_username);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        usernameText = new Text(parent, SWT.BORDER);
        GridData usernameGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        usernameGd.widthHint = UIUtils.getFontHeight(parent) * 20;
        usernameText.setLayoutData(usernameGd);
        usernameText.addModifyListener(e -> propertyChangeListener.run());

        Label credentialType = UIUtils.createControlLabel(parent, DSQLMessages.label_use_credentials);
        credentialType.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Composite credentialTypeGroup = UIUtils.createComposite(parent, 3);
        typeProfileRadio = UIUtils.createRadioButton(credentialTypeGroup, DSQLMessages.radio_aws_profile, true, credentialTypeSwitcher);
        typeCredentialsRadio = UIUtils.createRadioButton(credentialTypeGroup, DSQLMessages.radio_aws_credentials, false, credentialTypeSwitcher);
        typeTokenRadio = UIUtils.createRadioButton(credentialTypeGroup, DSQLMessages.radio_password, false, credentialTypeSwitcher);

        accessKeyLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_aws_access_key);
        accessKeyText = new Text(parent, SWT.BORDER);
        setupLabeledText(parent, propertyChangeListener, accessKeyLabel, accessKeyText);
        secretKeyLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_aws_secret_key);
        secretKeyText = new Text(parent, SWT.BORDER);
        setupLabeledText(parent, propertyChangeListener, secretKeyLabel, secretKeyText);
        sessionTokenLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_aws_session_token);
        sessionTokenText = new Text(parent, SWT.BORDER);
        setupLabeledText(parent, propertyChangeListener, sessionTokenLabel, sessionTokenText);
        sessionCredentialWidgets = List.of(accessKeyLabel, accessKeyText, secretKeyLabel, secretKeyText, sessionTokenLabel, sessionTokenText);

        profileLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_aws_profile);
        profileText = new Text(parent, SWT.BORDER);
        setupLabeledText(parent, propertyChangeListener, profileLabel, profileText);
        profileCredentialWidgets = List.of(profileLabel, profileText);

        tokenLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_dsql_token);
        tokenText = new Text(parent, SWT.BORDER|SWT.PASSWORD);
        setupLabeledText(parent, propertyChangeListener, tokenLabel, tokenText);
        tokenCredentialWidgets = List.of(tokenLabel, tokenText);

        regionLabel = UIUtils.createControlLabel(parent, DSQLMessages.label_aws_region);
        regionLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        regionCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData regionGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        regionGd.widthHint = UIUtils.getFontHeight(parent) * 15;
        regionCombo.setLayoutData(regionGd);
        regionCombo.addModifyListener(e -> propertyChangeListener.run());
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dbpDataSourceContainer) {
        this.dataSource = dbpDataSourceContainer;
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();

        loadText(usernameText, configuration.getUserName());

        if (this.regionCombo != null && !this.regionCombo.isDisposed()) {
            this.regionCombo.setItems(DSQLConstants.AWS_REGIONS);
            this.regionCombo.setText(DSQLConstants.DEFAULT_REGION);
        }

        loadText(accessKeyText, configuration.getProperty(DSQLConstants.AWS_ACCESS_KEY));
        loadText(secretKeyText, configuration.getProperty(DSQLConstants.AWS_SECRET_KEY));
        loadText(sessionTokenText, configuration.getProperty(DSQLConstants.AWS_SESSION_TOKEN));
        String savedProfile = configuration.getProperty(DSQLConstants.AWS_PROFILE);
        loadText(profileText, savedProfile == null || savedProfile.isEmpty() ? DSQLConstants.DEFAULT_PROFILE : savedProfile);
        loadText(tokenText, configuration.getProperty(DSQLConstants.DSQL_TOKEN));

        setSelectedAuthType(configuration.getProperty(DSQLConstants.AUTH_TYPE));
        credentialTypeSwitcher.widgetSelected(null);
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dbpDataSourceContainer) {
        DBPConnectionConfiguration configuration = dbpDataSourceContainer.getConnectionConfiguration();
        configuration.setUserName(trimText(usernameText));
        configuration.setProperty(DSQLConstants.AWS_REGION, regionCombo.getText());

        if (this.typeCredentialsRadio.getSelection()) {
            // Use AWS Creds
            configuration.setProperty(DSQLConstants.AUTH_TYPE, DSQLConstants.AUTH_TYPES.AWS_SESSION_CREDENTIALS.toString());
            configuration.setProperty(DSQLConstants.AWS_ACCESS_KEY, trimText(accessKeyText));
            configuration.setProperty(DSQLConstants.AWS_SECRET_KEY, trimText(secretKeyText));
            configuration.setProperty(DSQLConstants.AWS_SESSION_TOKEN, trimText(sessionTokenText));
        } else if (this.typeProfileRadio.getSelection()) {
            // Use Profile
            configuration.setProperty(DSQLConstants.AUTH_TYPE, DSQLConstants.AUTH_TYPES.AWS_PROFILE.toString());
            configuration.setProperty(DSQLConstants.AWS_PROFILE, trimText(profileText));
        } else if (this.typeTokenRadio.getSelection()) {
            // Use dsql token
            configuration.setProperty(DSQLConstants.AUTH_TYPE, DSQLConstants.AUTH_TYPES.DSQL_TOKEN.toString());
            configuration.setProperty(DSQLConstants.DSQL_TOKEN, trimText(tokenText));
        }
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dbpDataSourceContainer) {
        loadSettings(dbpDataSourceContainer);
    }

    @Override
    public boolean isComplete() {
        boolean isComplete = false;

        if (this.typeCredentialsRadio.getSelection()) {
            isComplete = accessKeyText != null && secretKeyText != null && sessionTokenText != null &&
                !CommonUtils.isEmpty(accessKeyText.getText()) &&
                !CommonUtils.isEmpty(secretKeyText.getText()) &&
                !CommonUtils.isEmpty(sessionTokenText.getText());
        } else if (this.typeProfileRadio.getSelection()) {
            isComplete = profileText != null && !CommonUtils.isEmpty(profileText.getText());
        } else {
            isComplete = tokenText != null && !CommonUtils.isEmpty(tokenText.getText());
        }

        return isComplete && usernameText != null && !CommonUtils.isEmpty(usernameText.getText());
    }


    // Helper methods

    private void setupLabeledText(Composite parent, Runnable propertyChangeListener, Label inputLabel, Text inputText) {
        inputLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(parent) * 25;
        inputText.setLayoutData(gd);
        inputText.addModifyListener(e -> propertyChangeListener.run());
    }

    private String trimText(Text text) {
        return text != null ? text.getText().trim() : "";
    }

    private void loadText(Text text, String value) {
        if (text != null && !text.isDisposed()) {
            text.setText(CommonUtils.notEmpty(value));
        }
    }

    private void setSelectedAuthType(String authType) {
        if (CommonUtils.isEmpty(authType) || authType.equals(DSQLConstants.AUTH_TYPES.AWS_PROFILE.toString())) {
            typeProfileRadio.setSelection(true);
        } else if (authType.equals(DSQLConstants.AUTH_TYPES.AWS_SESSION_CREDENTIALS.toString())) {
            typeCredentialsRadio.setSelection(true);
        } else if (authType.equals(DSQLConstants.AUTH_TYPES.DSQL_TOKEN.toString())) {
            typeTokenRadio.setSelection(true);
        }
    }

    private void hideWidgets(List<Control> widgets, boolean visible) {
        for (Control widget : widgets) {
            ((GridData) widget.getLayoutData()).exclude = !visible;
            widget.setVisible(visible);
        }
    }

}
