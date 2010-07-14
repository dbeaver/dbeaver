package org.jkiss.dbeaver.ext.mysql.views;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.runtime.FileLocator;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.Activator;
import org.jkiss.dbeaver.ext.ui.IDataSourceEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.controls.proptree.DriverPropertiesControl;

/**
 * ConnectionEditorPage
 */
public class MySQLConnectionPage extends DialogPage implements IDataSourceEditor
{
    static final Log log = LogFactory.getLog(MySQLConnectionPage.class);

    private IDataSourceEditorSite site;
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;
    private DriverPropertiesControl driverProps;
    private Button testButton;
    private Image logoImage;

    @Override
    public void dispose()
    {
        if (logoImage != null) {
            logoImage.dispose();
            logoImage = null;
        }
        super.dispose();
    }

    public void createControl(Composite composite)
    {
        logoImage = Activator.getImageDescriptor("icons/mysql_logo.png").createImage();


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
        driverProps = new DriverPropertiesControl(optionsFolder, SWT.NONE);
        propsTab.setControl(driverProps);

        optionsFolder.addSelectionListener(
            new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    if (e.item == propsTab) {
                        //refreshDriverProperties();
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
        GridLayout gl = new GridLayout(3, false);
        gl.marginHeight = 20;
        gl.marginWidth = 20;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = new Label(addrGroup, SWT.NONE);
        hostLabel.setText("Server Host:");
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label portLabel = new Label(addrGroup, SWT.NONE);
        portLabel.setText("Port:");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
        portLabel.setLayoutData(gd);

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addModifyListener(textListener);

        Label logoLabel = new Label(addrGroup, SWT.NONE);
        logoLabel.setImage(logoImage);
        gd = new GridData();
        gd.verticalSpan = 4;
        logoLabel.setLayoutData(gd);

        Label dbLabel = new Label(addrGroup, SWT.NONE);
        dbLabel.setText("Database:");
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        //gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

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

    public void setSite(IDataSourceEditorSite site)
    {
        this.site = site;
    }

    public boolean isComplete()
    {
        return hostText != null && portText != null && 
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(portText.getText());
    }

    public void loadSettings()
    {
        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            if (hostText != null) {
                hostText.setText(CommonUtils.getString(connectionInfo.getHostName()));
            }
            if (portText != null) {
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(CommonUtils.getString(connectionInfo.getHostPort()));
                } else {
                    portText.setText(String.valueOf(MySQLConstants.DEFAULT_PORT));
                }
            }
            if (dbText != null) {
                dbText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
            }
            if (usernameText != null) {
                usernameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
            }
            if (passwordText != null) {
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
            }
        } else {
            if (portText != null) {
                portText.setText(String.valueOf(MySQLConstants.DEFAULT_PORT));
            }
        }

        // Set props model
        if (driverProps != null) {
            refreshDriverProperties();
        }
    }

    private void refreshDriverProperties()
    {
        DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
        saveSettings(tmpConnectionInfo);
        driverProps.loadProperties(site.getDriver(), tmpConnectionInfo.getJdbcURL(), site.getConnectionInfo().getProperties());
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
            connectionInfo.setProperties(driverProps.getProperties());
            connectionInfo.setJdbcURL(
                "jdbc:mysql://" + connectionInfo.getHostName() +
                    ":" + connectionInfo.getHostPort() +
                    "/" + connectionInfo.getDatabaseName());
        }
    }

    private void evaluateURL()
    {
        site.updateButtons();
        if (testButton != null) {
            testButton.setEnabled(this.isComplete());
        }
    }

}
