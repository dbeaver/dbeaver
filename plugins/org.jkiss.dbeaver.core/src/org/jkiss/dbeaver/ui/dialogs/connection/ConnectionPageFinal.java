/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
import org.jkiss.utils.CommonUtils;

import java.util.StringTokenizer;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

class ConnectionPageFinal extends ActiveWizardPage {
    static final Log log = LogFactory.getLog(ConnectionPageFinal.class);

    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private Button savePasswordCheck;
    private Button showSystemObjects;
    private Button readOnlyConnection;
    private Text catFilterText;
    private Text schemaFilterText;
    private Font boldFont;

    private boolean connectionNameChanged = false;
    private Button tunnelButton;
    private Button eventsButton;

    ConnectionPageFinal(ConnectionWizard wizard)
    {
        super("newConnectionFinal"); //$NON-NLS-1$
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_connection_wizard_final_header);
        setDescription(CoreMessages.dialog_connection_wizard_final_description);
    }

    ConnectionPageFinal(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(boldFont);
        super.dispose();
    }

    @Override
    public void activatePage()
    {
        if (connectionNameText != null) {
            ConnectionPageSettings settings = wizard.getPageSettings();
            if (settings != null && connectionNameText != null && (CommonUtils.isEmpty(connectionNameText.getText()) || !connectionNameChanged)) {
                DBPConnectionInfo connectionInfo = settings.getConnectionInfo();
                String newName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
                if (CommonUtils.isEmpty(newName)) {
                    newName = connectionInfo.getDatabaseName();
                    if (CommonUtils.isEmpty(newName)) {
                        newName = connectionInfo.getHostName();
                    }
                    if (CommonUtils.isEmpty(newName)) {
                        newName = connectionInfo.getUrl();
                    }
                    if (CommonUtils.isEmpty(newName)) {
                        newName = CoreMessages.dialog_connection_wizard_final_default_new_connection_name;
                    }
                    StringTokenizer st = new StringTokenizer(newName, "/\\:,?=%$#@!^&*()"); //$NON-NLS-1$
                    while (st.hasMoreTokens()) {
                        newName = st.nextToken();
                    }
                    if (!CommonUtils.isEmpty(settings.getDriver().getCategory())) {
                        newName = settings.getDriver().getCategory() + " - " + newName; //$NON-NLS-1$
                    } else {
                        newName = settings.getDriver().getName() + " - " + newName; //$NON-NLS-1$
                    }
                    newName = CommonUtils.truncateString(newName, 50);
                }
                connectionNameText.setText(newName);
                connectionNameChanged = false;

                for (DBWHandlerConfiguration config : connectionInfo.getDeclaredHandlers()) {
                    if (config.isEnabled()) {
                        tunnelButton.setFont(boldFont);
                        break;
                    }
                }
                for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                    if (connectionInfo.getEvent(eventType).isEnabled()) {
                        eventsButton.setFont(boldFont);
                        break;
                    }
                }
            }
        }
        if (dataSourceDescriptor != null) {
            savePasswordCheck.setSelection(dataSourceDescriptor.isSavePassword());
            showSystemObjects.setSelection(dataSourceDescriptor.isShowSystemObjects());
            readOnlyConnection.setSelection(dataSourceDescriptor.isConnectionReadOnly());
            //catFilterText.setText(CommonUtils.getString(dataSourceDescriptor.getCatalogFilter()));
            //schemaFilterText.setText(CommonUtils.getString(dataSourceDescriptor.getSchemaFilter()));
        }
        long features = 0;
        try {
            features = wizard.getPageSettings().getDriver().getDataSourceProvider().getFeatures();
        } catch (DBException e) {
            log.error("Can't obtain data source provider instance", e); //$NON-NLS-1$
        }
        //UIUtils.enableCheckText(catFilterText, (features & DBPDataSourceProvider.FEATURE_CATALOGS) != 0);
        //UIUtils.enableCheckText(schemaFilterText, (features & DBPDataSourceProvider.FEATURE_SCHEMAS) != 0);
    }

    @Override
    public void deactivatePage()
    {
    }

    @Override
    public void createControl(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        //gl.marginHeight = 20;
        //gl.marginWidth = 20;
        //gl.verticalSpacing = 10;
        group.setLayout(gl);
        GridData gd;// = new GridData(GridData.FILL_HORIZONTAL);
        //gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_CENTER;
        //gd.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        //group.setLayoutData(gd);

        String connectionName = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName(); //$NON-NLS-1$
        connectionNameText = UIUtils.createLabelText(group, CoreMessages.dialog_connection_wizard_final_label_connection_name, CommonUtils.toString(connectionName));
        connectionNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                connectionNameChanged = true;
                ConnectionPageFinal.this.getContainer().updateButtons();
            }
        });

        {
            Group securityGroup = UIUtils.createControlGroup(group, CoreMessages.dialog_connection_wizard_final_group_security, 1, GridData.FILL_HORIZONTAL, 0);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            gd.widthHint = 400;
            securityGroup.setLayoutData(gd);

            savePasswordCheck = UIUtils.createCheckbox(securityGroup, CoreMessages.dialog_connection_wizard_final_checkbox_save_password_locally, dataSourceDescriptor == null || dataSourceDescriptor.isSavePassword());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            //gd.horizontalSpan = 2;
            savePasswordCheck.setLayoutData(gd);
        }

        {
            Group miscGroup = UIUtils.createControlGroup(group, CoreMessages.dialog_connection_wizard_final_group_misc, 2, GridData.FILL_HORIZONTAL, 0);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            miscGroup.setLayoutData(gd);

            showSystemObjects = UIUtils.createCheckbox(miscGroup, CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects, dataSourceDescriptor == null || dataSourceDescriptor.isShowSystemObjects());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            showSystemObjects.setLayoutData(gd);

            readOnlyConnection = UIUtils.createCheckbox(miscGroup, CoreMessages.dialog_connection_wizard_final_checkbox_connection_readonly, dataSourceDescriptor == null || dataSourceDescriptor.isConnectionReadOnly());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            readOnlyConnection.setLayoutData(gd);
        }

        {
            //Composite buttonsGroup = UIUtils.createPlaceholder(group, 3);
            Composite buttonsGroup = new Composite(group, SWT.NONE);
            gl = new GridLayout(3, false);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 10;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            buttonsGroup.setLayout(gl);

            //buttonsGroup.setLayout(new GridLayout(2, true));
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            buttonsGroup.setLayoutData(gd);

//            String catFilter = dataSourceDescriptor == null ? null : dataSourceDescriptor.getCatalogFilter();
//            catFilterText = UIUtils.createCheckText(buttonsGroup, CoreMessages.dialog_connection_wizard_final_checkbox_filter_catalogs, CommonUtils.getString(catFilter), !CommonUtils.isEmpty(catFilter), 200);
//
//            String schFilter = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getSchemaFilter(); //$NON-NLS-1$
//            schemaFilterText = UIUtils.createCheckText(buttonsGroup, CoreMessages.dialog_connection_wizard_final_checkbox_filter_schemas, CommonUtils.getString(schFilter), !CommonUtils.isEmpty(schFilter), 200);

            {
                Button filtersButton = new Button(buttonsGroup, SWT.PUSH);
                filtersButton.setText("Object Filters ...");
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.grabExcessVerticalSpace = true;
                filtersButton.setLayoutData(gd);
                filtersButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {

                    }
                });
            }
            {
                tunnelButton = new Button(buttonsGroup, SWT.PUSH);
                tunnelButton.setText("Tunneling ...");
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.grabExcessVerticalSpace = true;
                tunnelButton.setLayoutData(gd);
                tunnelButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        configureTunnels();
                    }
                });
            }

            {
                eventsButton = new Button(buttonsGroup, SWT.PUSH);
                eventsButton.setText(CoreMessages.dialog_connection_wizard_final_button_events);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.grabExcessHorizontalSpace = true;
                eventsButton.setLayoutData(gd);
                eventsButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        configureEvents();
                    }
                });
            }
        }

        setControl(group);

        UIUtils.setHelp(group, IHelpContextIds.CTX_CON_WIZARD_FINAL);
    }

    @Override
    public boolean isPageComplete()
    {
        return connectionNameText != null &&
            !CommonUtils.isEmpty(connectionNameText.getText());
    }

    void saveSettings(DataSourceDescriptor dataSource)
    {
        dataSource.setName(connectionNameText.getText());
        dataSource.setSavePassword(savePasswordCheck.getSelection());
        dataSource.setShowSystemObjects(showSystemObjects.getSelection());
        dataSource.setConnectionReadOnly(readOnlyConnection.getSelection());
        if (!dataSource.isSavePassword()) {
            dataSource.resetPassword();
        }
        //dataSource.setCatalogFilter(catFilterText == null || !catFilterText.isEnabled() ? null : catFilterText.getText());
        //dataSource.setSchemaFilter(schemaFilterText == null || !schemaFilterText.isEnabled() ? null : schemaFilterText.getText());
    }

    private void configureEvents()
    {
        EditEventsDialog dialog = new EditEventsDialog(
            getShell(),
            wizard.getPageSettings().getConnectionInfo());
        dialog.open();
    }

    private void configureTunnels()
    {
        EditTunnelDialog dialog = new EditTunnelDialog(
            getShell(),
            wizard.getPageSettings().getDriver(),
            wizard.getPageSettings().getConnectionInfo());
        dialog.open();
    }

}