/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.driver.DriverDependencies;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * DriverEditDialog
 */
public class DriverLibraryDetailsDialog extends HelpEnabledDialog
{
    private static final String DIALOG_ID = "DBeaver.DriverLibraryDetailsDialog";//$NON-NLS-1$

    private DBPDriver driver;
    private DBPDriverLibrary library;

    public DriverLibraryDetailsDialog(Shell shell, DBPDriver driver, DBPDriverLibrary library)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.driver = driver;
        this.library = library;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Driver '" + driver.getName() + "' library '" + library.getDisplayName() + "'"); //$NON-NLS-2$
        getShell().setImage(DBeaverIcons.getImage(library.getIcon()));

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        group.setLayoutData(gd);

        Group propsGroup = UIUtils.createControlGroup(group, "Information", 2, -1, -1);
        propsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createLabelText(propsGroup, "Driver", driver.getName(), SWT.BORDER | SWT.READ_ONLY);
        UIUtils.createLabelText(propsGroup, "Library", library.getDisplayName(), SWT.BORDER | SWT.READ_ONLY);
        UIUtils.createLabelText(propsGroup, "Path", library.getPath(), SWT.BORDER | SWT.READ_ONLY);
        UIUtils.createLabelText(propsGroup, "Version", library.getVersion(), SWT.BORDER | SWT.READ_ONLY);
        Text fileText = UIUtils.createLabelText(propsGroup, "File", "", SWT.BORDER | SWT.READ_ONLY);

        TabFolder tabs = new TabFolder(group, SWT.HORIZONTAL | SWT.FLAT);
        tabs.setLayoutData(new GridData(GridData.FILL_BOTH));

        createDependenciesTab(tabs);
        createLicenseTab(tabs);
        createDetailsTab(tabs);

        final File localFile = library.getLocalFile();
        if (localFile != null) {
            fileText.setText(localFile.getAbsolutePath());
        }

        return group;
    }

    private void createDependenciesTab(TabFolder tabs) {
        Composite paramsGroup = new Composite(tabs, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        final Set<DBPDriverLibrary> libList = Collections.singleton(library);
        DriverDependencies dependencies = new DriverDependencies(libList);
        final DriverDependenciesTree depsTree = new DriverDependenciesTree(
            paramsGroup,
            DBeaverUI.getDefaultRunnableContext(),
            dependencies,
            driver,
            libList,
            false);
        depsTree.resolveLibraries();
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                depsTree.resizeTree();
            }
        });

        TabItem depsTab = new TabItem(tabs, SWT.NONE);
        depsTab.setText("Dependencies");
        depsTab.setToolTipText("Library dependencies");
        depsTab.setControl(paramsGroup);
    }

    private void createDetailsTab(TabFolder tabs) {
        Composite detailsGroup = new Composite(tabs, SWT.NONE);
        detailsGroup.setLayout(new GridLayout(1, false));

        UIUtils.createControlLabel(detailsGroup, "Description");
        Text descriptionText = new Text(detailsGroup, SWT.READ_ONLY | SWT.BORDER);
        descriptionText.setText(CommonUtils.notEmpty(library.getDescription()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 40;
        descriptionText.setLayoutData(gd);

        TabItem detailsTab = new TabItem(tabs, SWT.NONE);
        detailsTab.setText("Details");
        detailsTab.setToolTipText("Additional library information");
        detailsTab.setControl(detailsGroup);
    }


    /*
        private void createParametersTab(TabFolder group)
        {
            Composite paramsGroup = new Composite(group, SWT.NONE);
            paramsGroup.setLayout(new GridLayout(1, false));

            parametersEditor = new PropertyTreeViewer(paramsGroup, SWT.BORDER);
            driverPropertySource = new PropertySourceCustom(
                driver.getProviderDescriptor().getDriverProperties(),
                driver.getDriverParameters());
            driverPropertySource.setDefaultValues(driver.getDefaultDriverParameters());
            parametersEditor.loadProperties(driverPropertySource);

            TabItem paramsTab = new TabItem(group, SWT.NONE);
            paramsTab.setText(CoreMessages.dialog_edit_driver_tab_name_advanced_parameters);
            paramsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_tooltip_advanced_parameters);
            paramsTab.setControl(paramsGroup);
        }

        private void createConnectionPropertiesTab(TabFolder group)
        {
            Composite paramsGroup = new Composite(group, SWT.NONE);
            paramsGroup.setLayout(new GridLayout(1, false));

            connectionPropertiesEditor = new ConnectionPropertiesControl(paramsGroup, SWT.BORDER);
            connectionPropertySource = connectionPropertiesEditor.makeProperties(driver, driver.getConnectionProperties());
            connectionPropertiesEditor.loadProperties(connectionPropertySource);


            TabItem paramsTab = new TabItem(group, SWT.NONE);
            paramsTab.setText(CoreMessages.dialog_edit_driver_tab_name_connection_properties);
            paramsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_tooltip_connection_properties);
            paramsTab.setControl(paramsGroup);
        }

        private void createClientHomesTab(TabFolder group)
        {
            clientHomesPanel = new ClientHomesPanel(group, SWT.NONE);
            clientHomesPanel.loadHomes(driver);
            clientHomesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            TabItem paramsTab = new TabItem(group, SWT.NONE);
            paramsTab.setText(CoreMessages.dialog_edit_driver_tab_name_client_homes);
            paramsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_name_client_homes);
            paramsTab.setControl(clientHomesPanel);
        }
    */
    private void createLicenseTab(TabFolder group)
    {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        Text licenseText = new Text(paramsGroup, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
        licenseText.setText("License");
        licenseText.setEditable(false);
        licenseText.setMessage(CoreMessages.dialog_edit_driver_text_driver_license);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        //gd.grabExcessVerticalSpace = true;
        licenseText.setLayoutData(gd);

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(CoreMessages.dialog_edit_driver_tab_name_license);
        paramsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_tooltip_license);
        paramsTab.setControl(paramsGroup);
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.okPressed();
    }
}
