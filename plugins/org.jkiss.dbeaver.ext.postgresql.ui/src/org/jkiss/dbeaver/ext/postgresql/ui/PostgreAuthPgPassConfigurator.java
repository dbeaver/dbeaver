/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * PgPass UI config (user name only)
 */
public class PostgreAuthPgPassConfigurator implements IObjectPropertyConfigurator<DBPDataSourceContainer> {

    protected Text usernameText;

    @Override
    public void createControl(Composite authPanel, Runnable propertyChangeListener) {
        int fontHeight = UIUtils.getFontHeight(authPanel);

        Label usernameLabel = UIUtils.createLabel(authPanel, UIConnectionMessages.dialog_connection_auth_label_username);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        usernameText = new Text(authPanel, SWT.BORDER);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(e -> propertyChangeListener.run());
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        this.usernameText.setText(CommonUtils.notEmpty(dataSource.getConnectionConfiguration().getUserName()));
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setUserName(this.usernameText.getText());
        dataSource.setSavePassword(true);
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dataSource) {
        loadSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return true;
    }


}
