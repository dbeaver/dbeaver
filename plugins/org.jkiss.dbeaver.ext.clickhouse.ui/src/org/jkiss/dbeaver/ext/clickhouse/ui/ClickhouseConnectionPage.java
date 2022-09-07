/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.ui;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.internal.GenericMessages;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class ClickhouseConnectionPage extends GenericConnectionPage {
    private static final Log log = Log.getLog(ClickhouseConnectionPage.class);
    private Button userSSLCheck;
    private static final String SSL_PARAM = "ssl"; //$NON-NLS-1$

    @Override
    protected void addAdditionalGeneralControls(Composite settingsGroup) {
        DBPPropertyDescriptor sslProperty = null;
        try {
            sslProperty = getSSLProperty();
        } catch (DBException e) {
            log.warn("Can't get connection properties");
        }
        if (sslProperty != null) {
            Composite sslComposite = UIUtils.createComposite(settingsGroup, 2);
            sslComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            userSSLCheck = UIUtils.createCheckbox(sslComposite,
                GenericMessages.dialog_connection_page_checkbox_use_ssl,
                GenericMessages.dialog_connection_page_checkbox_tip_use_ssl,
                false,
                4
            );
            userSSLCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getDriverPropertiesDialogPage().getPropertySource().setPropertyValue(new VoidProgressMonitor(),
                        SSL_PARAM, userSSLCheck.getSelection() ? "true" : "false"); //$NON-NLS-1$
                }
            });
            userSSLCheck.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        }
    }

    protected DBPPropertyDescriptor getSSLProperty() throws DBException {
        DBPPropertyDescriptor[] connectionProperties = site.getDriver()
            .getDataSourceProvider()
            .getConnectionProperties(new VoidProgressMonitor(), site.getDriver(), new DBPConnectionConfiguration());
        boolean b = false;
        for (DBPPropertyDescriptor it : connectionProperties) {
            if (SSL_PARAM.equals(it.getId())) {
                return it;
            }
        }
        return null;
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        UIUtils.syncExec(() -> {
            String ssl = site.getActiveDataSource().getConnectionConfiguration().getProperty(SSL_PARAM);
            if (!CommonUtils.isEmpty(ssl)) {
                userSSLCheck.setSelection(ssl.equals("true")); //$NON-NLS-1$
            }
        });
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        dataSource.getConnectionConfiguration()
            .setProperty(SSL_PARAM, userSSLCheck.getSelection() ? "true" : "false"); //$NON-NLS-1$
    }
}