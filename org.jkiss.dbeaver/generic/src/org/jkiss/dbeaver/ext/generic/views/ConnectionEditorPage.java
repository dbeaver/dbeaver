/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.views;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.controls.proptree.DriverPropertiesControl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConnectionEditorPage
 */
public class ConnectionEditorPage extends DialogPage implements IDataSourceConnectionEditor
{
    static final Log log = LogFactory.getLog(ConnectionEditorPage.class);

    private static final String PROP_HOST = "host";
    private static final String PROP_PORT = "port";
    private static final String PROP_DATABASE = "database";

    private static final String PATTERN_HOST = "{" + PROP_HOST + "}";
    private static final String PATTERN_PORT = "{" + PROP_PORT + "}";
    private static final String PATTERN_DATABASE = "{" + PROP_DATABASE + "}";

    private IDataSourceConnectionEditorSite site;
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;
    private Text urlText;
    private Button testButton;

    private boolean isCustom;
    private List<String> urlComponents = new ArrayList<String>();
    private Set<String> requiredProperties = new HashSet<String>();

    private boolean driverPropsLoaded;
    private DriverPropertiesControl propsControl;

    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));

        TabFolder optionsFolder = new TabFolder(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        optionsFolder.setLayoutData(gd);

        TabItem addrTab = new TabItem(optionsFolder, SWT.NONE);
        addrTab.setText("General");
        addrTab.setToolTipText("General connection properties");
        addrTab.setControl(createGeneralTab(optionsFolder));

        final TabItem propsTab = new TabItem(optionsFolder, SWT.NONE);
        propsTab.setText("Advanced");
        propsTab.setToolTipText("Advanced/custom driver properties");
        propsTab.setControl(createPropertiesTab(optionsFolder));

        optionsFolder.addSelectionListener(
            new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    if (e.item == propsTab) {
                        refreshDriverProperties();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            }
        );
        setControl(optionsFolder);
    }

    private Composite createGeneralTab(Composite parent)
    {
        ModifyListener textListener = new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                evaluateURL();
            }
        };

        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        {
            Label urlLabel = new Label(addrGroup, SWT.NONE);
            urlLabel.setText("JDBC URL:");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            urlLabel.setLayoutData(gd);

            urlText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            urlText.setLayoutData(gd);
            urlText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    site.updateButtons();
                    testButton.setEnabled(isComplete());
                }
            });
        }

        Label hostLabel = new Label(addrGroup, SWT.NONE);
        hostLabel.setText("Server Host:");
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label portLabel = new Label(addrGroup, SWT.NONE);
        portLabel.setText("Port:");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        portLabel.setLayoutData(gd);

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.CENTER);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addModifyListener(textListener);

        Label dbLabel = new Label(addrGroup, SWT.NONE);
        dbLabel.setText("Database/Schema:");
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        //gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        Label emptyLabel = new Label(addrGroup, SWT.NONE);
        emptyLabel.setText("");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 2;
        gd.verticalSpan = 3;
        emptyLabel.setLayoutData(gd);

        Label usernameLabel = new Label(addrGroup, SWT.NONE);
        usernameLabel.setText("Username:");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        //gd.horizontalSpan = 3;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        Label passwordLabel = new Label(addrGroup, SWT.NONE);
        passwordLabel.setText("Password:");
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);

        Button driverButton = new Button(addrGroup, SWT.PUSH);
        driverButton.setText("Edit Driver Settings");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        driverButton.setLayoutData(gd);
        driverButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                if (site.openDriverEditor()) {
                    parseSampleURL(site.getDriver());
                    evaluateURL();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });

        testButton = new Button(addrGroup, SWT.PUSH);
        testButton.setText("Test Connection ... ");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 3;
        testButton.setLayoutData(gd);
        testButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                site.testConnection();
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        testButton.setEnabled(false);
        return addrGroup;
    }

    private DriverPropertiesControl createPropertiesTab(Composite parent)
    {
        propsControl = new DriverPropertiesControl(parent, SWT.NONE);
/*
        Composite propsGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        propsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        propsGroup.setLayoutData(gd);

        propsTree = new TreeViewer(propsGroup, SWT.BORDER);
        propsTree.setContentProvider(new PropsContentProvider());
        //propsTree.setLabelProvider(new PropsLabelProvider());
        gd = new GridData(GridData.FILL_BOTH);
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.minimumHeight = 120;
        propsTree.getTree().setLayoutData(gd);
        propsTree.getTree().setHeaderVisible(true);

        ColumnViewerToolTipSupport.enableFor(propsTree, ToolTip.NO_RECREATE);

        TreeViewerColumn column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(200);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Name");
        column.setLabelProvider(new PropsLabelProvider());

        column = new TreeViewerColumn(propsTree, SWT.NONE);
        column.getColumn().setWidth(120);
        column.getColumn().setMoveable(true);
        column.getColumn().setText("Value");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            public String getText(Object obj)
            {
                return "";
            }
        });
*/

        return propsControl;
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    public boolean isComplete()
    {
        if (isCustom) {
            return !CommonUtils.isEmpty(urlText.getText());
        } else {
            for (String prop : requiredProperties) {
                if (
                    (prop.equals(PROP_HOST) && CommonUtils.isEmpty(hostText.getText())) ||
                        (prop.equals(PROP_PORT) && CommonUtils.isEmpty(portText.getText())) ||
                        (prop.equals(PROP_DATABASE) && CommonUtils.isEmpty(dbText.getText()))) {
                    return false;
                }
            }
            return true;
        }
    }

    public void loadSettings()
    {
        // Load values from new connection info
        driverPropsLoaded = false;
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            this.parseSampleURL(site.getDriver());
            if (!isCustom) {
                if (hostText != null) {
                    if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                        hostText.setText(CommonUtils.getString(connectionInfo.getHostName()));
                    } else {
                        hostText.setText("localhost");
                    }
                }
                if (portText != null) {
                    if (connectionInfo.getHostPort() != null) {
                        portText.setText(CommonUtils.getString(connectionInfo.getHostPort()));
                    } else if (site.getDriver().getDefaultPort() != null) {
                        portText.setText(site.getDriver().getDefaultPort().toString());
                    } else {
                        portText.setText("");
                    }
                }
                if (dbText != null) {
                    dbText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
                }
            } else {
                hostText.setText("");
                portText.setText("");
                dbText.setText("");
            }
            if (usernameText != null) {
                usernameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
            }
            if (passwordText != null) {
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
            }

            if (urlText != null) {
                if (connectionInfo.getUrl() != null) {
                    urlText.setText(CommonUtils.getString(connectionInfo.getUrl()));
                } else {
                    if (!isCustom) {
                        evaluateURL();
                    } else {
                        urlText.setText("");
                    }
                }
            }
        }

        // Set props model
        if (propsControl != null) {
            refreshDriverProperties();
        }
    }

    private void refreshDriverProperties()
    {
        if (!driverPropsLoaded) {
            DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
            saveSettings(tmpConnectionInfo);
            tmpConnectionInfo.setProperties(site.getConnectionInfo().getProperties());
            propsControl.loadProperties(site.getDriver(), tmpConnectionInfo/*.getUrl(), site.getConnectionInfo().getProperties()*/);
            driverPropsLoaded = true;
        }
    }

    public void saveSettings()
    {
        saveSettings(site.getConnectionInfo());
    }

    private void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            if (hostText != null) {
                connectionInfo.setHostName(hostText.getText());
            }
            if (portText != null) {
                connectionInfo.setHostPort(portText.getText());
            }
            if (dbText != null) {
                connectionInfo.setDatabaseName(dbText.getText());
            }
            if (usernameText != null) {
                connectionInfo.setUserName(usernameText.getText());
            }
            if (passwordText != null) {
                connectionInfo.setUserPassword(passwordText.getText());
            }
            if (urlText != null) {
                connectionInfo.setUrl(urlText.getText());
            }
            connectionInfo.setProperties(propsControl.getProperties());
        }
    }

    private void parseSampleURL(DBPDriver driver)
    {
        this.urlComponents.clear();
        this.requiredProperties.clear();

        if (!CommonUtils.isEmpty(driver.getSampleURL())) {
            isCustom = false;
            String sampleURL = driver.getSampleURL();
            int offsetPos = 0;
            for (; ;) {
                int divPos = sampleURL.indexOf('[', offsetPos);
                if (divPos == -1) {
                    break;
                }
                int divPos2 = sampleURL.indexOf(']', divPos);
                if (divPos2 == -1) {
                    setErrorMessage("Bad sample URL: " + sampleURL);
                    break;
                }
                if (divPos > offsetPos) {
                    urlComponents.add(sampleURL.substring(offsetPos, divPos));
                }
                urlComponents.add(sampleURL.substring(divPos, divPos2 + 1));
                offsetPos = divPos2 + 1;
            }
            if (offsetPos < sampleURL.length() - 1) {
                urlComponents.add(sampleURL.substring(offsetPos));
            }
            // Check for required parts
            for (String component : urlComponents) {
                if (!component.startsWith("[")) {
                    int divPos = component.indexOf('{');
                    if (divPos != -1) {
                        int divPos2 = component.indexOf('}', divPos);
                        if (divPos2 != -1) {
                            String propName = component.substring(divPos + 1, divPos2);
                            requiredProperties.add(propName);
                        }
                    }
                }
            }
        } else {
            isCustom = true;
        }

        hostText.setEditable(!isCustom);
        portText.setEditable(!isCustom);
        dbText.setEditable(!isCustom);
        urlText.setEditable(isCustom);
    }

    private void evaluateURL()
    {
        if (!isCustom) {
            StringBuilder url = new StringBuilder();
            for (String component : urlComponents) {
                String newComponent = component;
                if (!CommonUtils.isEmpty(hostText.getText())) {
                    newComponent = newComponent.replace(PATTERN_HOST, hostText.getText());
                }
                if (!CommonUtils.isEmpty(portText.getText())) {
                    Integer defaultPort = site.getDriver().getDefaultPort();
                    if (defaultPort == null || !portText.getText().equals(defaultPort.toString())) {
                        newComponent = newComponent.replace(PATTERN_PORT, portText.getText());
                    }
                }
                if (!CommonUtils.isEmpty(dbText.getText())) {
                    newComponent = newComponent.replace(PATTERN_DATABASE, dbText.getText());
                }
                if (newComponent.startsWith("[")) {
                    if (!newComponent.equals(component)) {
                        url.append(newComponent.substring(1, newComponent.length() - 1));
                    }
                } else {
                    url.append(newComponent);
                }
            }
            urlText.setText(url.toString());
        }
        site.updateButtons();
        testButton.setEnabled(this.isComplete());
    }

}
