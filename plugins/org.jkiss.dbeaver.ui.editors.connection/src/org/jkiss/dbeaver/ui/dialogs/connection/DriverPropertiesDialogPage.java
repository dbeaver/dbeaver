/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * DriverPropertiesDialogPage
 */
public class DriverPropertiesDialogPage extends ConnectionPageAbstract
{

    private ConnectionPageAbstract hostPage;
    private ConnectionPropertiesControl propsControl;
    private PropertySourceCustom propertySource;

    private DBPConnectionConfiguration prevConnectionInfo = null;

    public DriverPropertiesDialogPage(ConnectionPageAbstract hostPage)
    {
        this.hostPage = hostPage;
        setTitle(UIConnectionMessages.dialog_setting_connection_driver_properties_title);
        setDescription(UIConnectionMessages.dialog_setting_connection_driver_properties_description);
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // Set props model
        if (visible && propsControl != null) {
            final DBPDataSourceContainer activeDataSource = site.getActiveDataSource();
            if (prevConnectionInfo == activeDataSource.getConnectionConfiguration()) {
                return;
            }

            final DBPConnectionConfiguration tmpConnectionInfo = new DBPConnectionConfiguration();
            final DataSourceDescriptor tempDataSource = site.getDataSourceRegistry()
                .createDataSource(
                    activeDataSource.getId(),
                    activeDataSource.getDriver(),
                    tmpConnectionInfo
                );

            hostPage.saveSettings(tempDataSource);
            tmpConnectionInfo.getProperties().putAll(activeDataSource.getConnectionConfiguration().getProperties());
            DBPDriverSubstitutionDescriptor driverSubstitution = activeDataSource.getDriverSubstitution();

            try {
                getSite().getRunnableContext().run(true, true, monitor -> {
                    monitor.beginTask("Loading driver properties", 1);
                    try {
                        propertySource = propsControl.makeProperties(
                            monitor,
                            getSite().getDriver(),
                            tmpConnectionInfo,
                            driverSubstitution);
                    } finally {
                        monitor.done();
                    }
                });
            } catch (InvocationTargetException e) {
                setErrorMessage(e.getTargetException().getMessage());
            } catch (InterruptedException e) {
                // ignore
            }
            if (propertySource != null) {
                propsControl.loadProperties(propertySource);
            }
            prevConnectionInfo = activeDataSource.getConnectionConfiguration();

            tempDataSource.dispose();
        }
    }

    @Override
    public void loadSettings()
    {
        // Do nothing
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (propsControl != null) {
            propsControl.saveEditorValues();
        }
        if (propertySource != null) {
            final Map<String, String> properties = dataSource.getConnectionConfiguration().getProperties();
            properties.clear();
            for (Map.Entry<String, Object> entry : propertySource.getPropertyValues().entrySet()) {
                String propName = CommonUtils.toString(entry.getKey());
                if (!propName.isEmpty()) {
                    properties.put(propName, CommonUtils.toString(entry.getValue()));
                }
            }
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite ph = UIUtils.createPlaceholder(parent, 1);
        if (parent.getLayout() instanceof GridLayout) {
            ph.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        propsControl = new ConnectionPropertiesControl(ph, SWT.NONE);
        Object layoutData = propsControl.getTree().getLayoutData();
        if (layoutData == null) {
            layoutData = new GridData(GridData.FILL_BOTH);
        }
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).heightHint = 200;
        }

        Composite linksComposite = UIUtils.createComposite(ph, 3);
        linksComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        propsControl.createPropertiesToolBar(linksComposite);

        {
            Control infoLabel = UIUtils.createInfoLabel(linksComposite, UIConnectionMessages.dialog_setting_connection_driver_properties_advanced);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.grabExcessHorizontalSpace = true;
            infoLabel.setLayoutData(gd);
            infoLabel.setToolTipText(UIConnectionMessages.dialog_setting_connection_driver_properties_advanced_tip);
        }
        {
            Link netConfigLink = new Link(linksComposite, SWT.NONE);
            if (!CommonUtils.isEmpty(site.getDriver().getWebURL())) {
                netConfigLink.setText("<a>" + UIConnectionMessages.dialog_setting_connection_driver_properties_docs_web_reference + "</a>");
                netConfigLink.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String url = site.getDriver().getPropertiesWebURL();
                        if (CommonUtils.isEmpty(url)) {
                            url = site.getDriver().getWebURL();
                        }
                        UIUtils.openWebBrowser(url);
                    }
                });
            }
            netConfigLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        }

        setControl(ph);
    }

}
