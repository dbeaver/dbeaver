package org.jkiss.dbeaver.ext.dm.ui.views;

import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.dm.ui.DmUIActivator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

public class DmConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

	private Text hostText;
	private Text portText;
	private Text dbText;
	//private Text usernameText;
	//private Text passwordText;
	private ClientHomesSelector homesSelector;
	private boolean activated = false;

	private Image logoImage;
	
	
	
	public DmConnectionPage() {
		logoImage = createImage("icons/dm_logo.png"); //$NON-NLS-1
	}

	@Override
	public void dispose() {
		super.dispose();
		UIUtils.dispose(logoImage);
	}

    @Override
    public Image getImage() {
        return logoImage;
    }
    
	@Override
	public void createControl(Composite composite) {
		ModifyListener textListener = e -> {
			if (activated) {
				site.updateButtons();
			}
		};

		final int fontHeight = UIUtils.getFontHeight(composite);
				
        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Group serverGroup = UIUtils.createControlGroup(addrGroup, "连接", 2, GridData.FILL_HORIZONTAL, 0);
        
                
        Label hostLabel = UIUtils.createControlLabel(serverGroup, "主机");
        Composite hostComposite = UIUtils.createComposite(serverGroup, 3);
        hostComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        hostText = new Text(hostComposite, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);
        
        portText = UIUtils.createLabelText(hostComposite, "端口", null, SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        ((GridData)portText.getLayoutData()).widthHint = fontHeight * 10;
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        dbText = UIUtils.createLabelText(serverGroup, "数据库", null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
        dbText.addModifyListener(textListener);
		
		createAuthPanel(addrGroup, 1);
		
		//UIUtils.createHorizontalLine(addrGroup, 2, 10); 创建分割线

		// 本地客户端
		Group advancedGroup = UIUtils.createControlGroup(addrGroup, "客户端", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
		
        homesSelector = new ClientHomesSelector(advancedGroup, "本地客户端", false);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        homesSelector.getPanel().setLayoutData(gd);
        
		createDriverPanel(addrGroup);
		setControl(addrGroup);
	}

	@Override
	public boolean isComplete() {
		return hostText != null && portText != null && !CommonUtils.isEmpty(hostText.getText())
				&& !CommonUtils.isEmpty(portText.getText());
	}

	
	@Override
	public void loadSettings() {
		super.loadSettings();
		DBPDriver driver = getSite().getDriver();
		//setImageDescriptor(DmUIActivator.getImageDescriptor("icons/dm_logo.png"));

		DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
		if (hostText != null) {
			if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
				hostText.setText(connectionInfo.getHostName());
			} else {
				hostText.setText("localhost");
			}
		}

		if (portText != null) {
			if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
				portText.setText(String.valueOf(connectionInfo.getHostPort()));
			} else if (getSite().isNew()) {
				if (driver.getDefaultPort() != null) {
					portText.setText(driver.getDefaultPort());
				} else {
					portText.setText("");
				}
			}
		}

		if (dbText != null) {
			dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
		}

		homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId(), site.isNew());
		
		activated = true;
	}

	@Override
	public void saveSettings(DBPDataSourceContainer dataSource) {
		DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
		if (hostText != null) {
			connectionInfo.setHostName(hostText.getText().trim());
		}
		if (portText != null) {
			connectionInfo.setHostPort(portText.getText().trim());
		}
		if (dbText != null) {
			connectionInfo.setDatabaseName(dbText.getText().trim());
		}
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }
		super.saveSettings(dataSource);
	}

	@Override
	public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
		return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
	}

}
