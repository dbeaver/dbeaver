/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDriverFileType;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ClientHomesPanel;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * DriverEditDialog
 */
public class DriverEditDialog extends HelpEnabledDialog
{
    static final Log log = LogFactory.getLog(DriverEditDialog.class);

    private DataSourceProviderDescriptor provider;
    private String defaultCategory;
    private DriverDescriptor driver;
    private String curFolder = null;
    private TableViewer libTable;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;
    private Combo classListCombo;
    private Button findClassButton;
    private Combo driverCategoryCombo;
    private Text driverNameText;
    private Text driverDescText;
    private Text driverClassText;
    private Text driverURLText;
    private Text driverPortText;
    private PropertyTreeViewer parametersEditor;
    private ConnectionPropertiesControl connectionPropertiesEditor;
    private List<DriverFileDescriptor> libList;
    private PropertySourceCustom driverPropertySource;
    private PropertySourceCustom connectionPropertySource;
    private ClientHomesPanel clientHomesPanel;
    //private Button anonymousCheck;

    public DriverEditDialog(Shell shell, DriverDescriptor driver)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.driver = driver;
        this.provider = driver.getProviderDescriptor();
    }

    public DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider, String category)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.provider = provider;
        this.defaultCategory = category;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        onChangeProperty();
        return ctl;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        if (driver == null) {
            getShell().setText(CoreMessages.dialog_edit_driver_title_create_driver);
            driver = provider.createDriver();
        } else {
            getShell().setText(CoreMessages.dialog_edit_driver_title_edit_driver + driver.getName() + "'"); //$NON-NLS-2$
            getShell().setImage(driver.getPlainIcon());
        }

        boolean isReadOnly = !provider.isDriversManagable();
        int advStyle = isReadOnly ? SWT.READ_ONLY : SWT.NONE;

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        group.setLayoutData(gd);

        {
            Composite propsGroup = new Composite(group, SWT.NONE);
            propsGroup.setLayout(new GridLayout(2, false));
            gd = new GridData(GridData.FILL_HORIZONTAL);
            propsGroup.setLayoutData(gd);

            UIUtils.createControlLabel(propsGroup, CoreMessages.dialog_edit_driver_label_driver_name);
            final Composite namePlaceholder = UIUtils.createPlaceholder(propsGroup, 3, 5);
            namePlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));

            driverNameText = new Text(namePlaceholder, SWT.BORDER | advStyle);
            driverNameText.setText(CommonUtils.getString(driver.getName()));
            driverNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            driverNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverCategoryCombo = UIUtils.createLabelCombo(namePlaceholder, CoreMessages.dialog_edit_driver_label_category, SWT.BORDER | SWT.DROP_DOWN | advStyle);
            {
                if (isReadOnly) {
                    driverCategoryCombo.setEnabled(false);
                }
                Set<String> categories = new HashSet<String>();
                for (DriverDescriptor drv : driver.getProviderDescriptor().getEnabledDrivers()) {
                    if (!CommonUtils.isEmpty(drv.getCategory())) {
                        categories.add(drv.getCategory());
                    }
                }
                for (String category : categories) {
                    driverCategoryCombo.add(category);
                }
                if (!CommonUtils.isEmpty(driver.getCategory())) {
                    driverCategoryCombo.setText(driver.getCategory());
                } else if (!CommonUtils.isEmpty(defaultCategory)) {
                    driverCategoryCombo.setText(defaultCategory);
                }
            }

            driverDescText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_description, CommonUtils.getString(driver.getDescription()), SWT.BORDER | advStyle);

            driverClassText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_class_name, CommonUtils.getString(driver.getDriverClassName()), SWT.BORDER | advStyle);
            driverClassText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverURLText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_sample_url, CommonUtils.getString(driver.getSampleURL()), SWT.BORDER | advStyle);
            driverURLText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            boolean hasSite = !CommonUtils.isEmpty(driver.getWebURL());

            UIUtils.createControlLabel(propsGroup, CoreMessages.dialog_edit_driver_label_default_port);
            Composite ph = hasSite ? UIUtils.createPlaceholder(propsGroup, 3) : propsGroup;
            driverPortText = new Text(ph, SWT.BORDER | advStyle);
            driverPortText.setText(driver.getDefaultPort() == null ? "" : driver.getDefaultPort()); //$NON-NLS-1$
            driverPortText.setLayoutData(new GridData(SWT.NONE));
            driverPortText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });
            if (hasSite) {
                GridLayout gl = (GridLayout)ph.getLayout();
                gl.horizontalSpacing = 5;

                UIUtils.createControlLabel(ph, CoreMessages.dialog_edit_driver_label_website);
                Link urlLabel = new Link(ph, SWT.NONE);
                urlLabel.setText("<a>" + (driver.getWebURL().length() > 32 ? CommonUtils.truncateString(driver.getWebURL(), 32) + "..." : driver.getWebURL()) + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
                urlLabel.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        Program.launch(driver.getWebURL());
                    }
                });
                //urlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            }
//            if (!isReadOnly) {
//                anonymousCheck = UIUtils.createLabelCheckbox(propsGroup, "Anonymous", driver.isAnonymousAccess(), SWT.NONE);
//                anonymousCheck.addSelectionListener(new SelectionAdapter() {
//                    @Override
//                    public void widgetSelected(SelectionEvent e)
//                    {
//                        onChangeProperty();
//                    }
//                });
//            }
        }

        final String license = driver.getLicense();
        {
            TabFolder tabFolder = new TabFolder(group, SWT.NONE);
            tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
            //tabFolder.setLayout(new FillLayout());

            createLibrariesTab(tabFolder);
            createConnectionPropertiesTab(tabFolder);
            createParametersTab(tabFolder);
            // Client homes
            if (driver.getClientManager() != null) {
                createClientHomesTab(tabFolder);
            }

            if (license != null) {
                createLicenseTab(tabFolder, license);
            }

            tabFolder.setSelection(0);
        }

        return group;
    }

    private void createLibrariesTab(TabFolder group)
    {
        GridData gd;
        Composite libsGroup = new Composite(group, SWT.NONE);
        libsGroup.setLayout(new GridLayout(2, false));

        //ListViewer list = new ListViewer(libsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            Composite libsListGroup = new Composite(libsGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            libsListGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            libsListGroup.setLayout(layout);
            //gd = new GridData(GridData.FILL_HORIZONTAL);

            // Additional libraries list
            libTable = new TableViewer(libsListGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            //libsTable.setLinesVisible (true);
            //libsTable.setHeaderVisible (true);
            libTable.setContentProvider(new ListContentProvider());
            libTable.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DriverFileDescriptor lib = (DriverFileDescriptor) cell.getElement();
                    cell.setText(lib.getPath());
                    if (lib.getFile().exists()) {
                        cell.setForeground(null);
                    } else {
                        cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    }
                    cell.setImage(
                        !lib.getFile().exists() ?
                            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE) :
                            lib.getFile().isDirectory() ?
                                DBIcon.TREE_FOLDER.getImage() :
                                (lib.getType() == DBPDriverFileType.jar ? DBIcon.JAR.getImage() : DBIcon.TYPE_UNKNOWN.getImage()));
                }
            });
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    changeLibSelection();
                }
            });

            libList = new ArrayList<DriverFileDescriptor>();
            for (DriverFileDescriptor lib : driver.getFiles()) {
                if (lib.isDisabled() || (lib.getType() != DBPDriverFileType.jar && lib.getType() != DBPDriverFileType.lib)) {
                    continue;
                }
                libList.add(lib);
            }
            libTable.setInput(libList);

            // Find driver class
            boolean isReadOnly = !provider.isDriversManagable();

            Composite findClassGroup = new Composite(libsListGroup, SWT.TOP);
            findClassGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            findClassGroup.setLayout(layout);

            UIUtils.createControlLabel(findClassGroup, CoreMessages.dialog_edit_driver_label_driver_class);
            classListCombo = new Combo(findClassGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            classListCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            classListCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = classListCombo.getSelectionIndex();
                    if (selIndex >= 0) {
                        driverClassText.setText(classListCombo.getItem(selIndex));
                    }
                }
            });
            classListCombo.setEnabled(!isReadOnly);
            findClassButton = new Button(findClassGroup, SWT.PUSH);
            findClassButton.setText(CoreMessages.dialog_edit_driver_button_bind_class);
            findClassButton.addListener(SWT.Selection, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    try {
                        ClassFindJob classFinder = new ClassFindJob();
                        new ProgressMonitorDialog(getShell()).run(true, true, classFinder);

                        if (classListCombo != null && !classListCombo.isDisposed()) {
                            java.util.List<String> classNames = classFinder.getDriverClassNames();
                            classListCombo.setItems(classNames.toArray(new String[classNames.size()]));
                            classListCombo.setListVisible(true);
                        }

                    } catch (InvocationTargetException e) {
                        log.error(e.getTargetException());
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            });
            findClassButton.setEnabled(!isReadOnly);
        }

        Composite libsControlGroup = new Composite(libsGroup, SWT.TOP);
        libsControlGroup.setLayout(new GridLayout(1, true));
        libsControlGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Button newButton = new Button(libsControlGroup, SWT.PUSH);
        newButton.setText(CoreMessages.dialog_edit_driver_button_add_file);
        newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        newButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                fd.setText(CoreMessages.dialog_edit_driver_dialog_open_driver_library);
                fd.setFilterPath(curFolder);
                String[] filterExt = {"*.jar", "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
                fd.setFilterExtensions(filterExt);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    String[] fileNames = fd.getFileNames();
                    if (!CommonUtils.isEmpty(fileNames)) {
                        File folderFile = new File(curFolder);
                        for (String fileName : fileNames) {
                            libList.add(
                                new DriverFileDescriptor(
                                    driver,
                                    fileName.endsWith(".jar") || fileName.endsWith(".zip") ? DBPDriverFileType.jar : DBPDriverFileType.lib,
                                    new File(folderFile, fileName).getAbsolutePath()));
                        }
                        changeLibContent();
                    }
                }
            }
        });

        Button newDirButton = new Button(libsControlGroup, SWT.PUSH);
        newDirButton.setText(CoreMessages.dialog_edit_driver_button_add_folder);
        newDirButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        newDirButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.MULTI);
                fd.setText(CoreMessages.dialog_edit_driver_dialog_open_driver_directory);
                fd.setFilterPath(curFolder);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    libList.add(new DriverFileDescriptor(
                        driver,
                        DBPDriverFileType.jar,
                        selected));
                    changeLibContent();
                }
            }
        });

        deleteButton = new Button(libsControlGroup, SWT.PUSH);
        deleteButton.setText(CoreMessages.dialog_edit_driver_button_delete);
        deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                libList.remove(getSelectedLibrary());
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

        upButton = new Button(libsControlGroup, SWT.PUSH);
        upButton.setText(CoreMessages.dialog_edit_driver_button_up);
        upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upButton.setEnabled(false);
        upButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                DriverFileDescriptor selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex - 1);
                changeLibContent();
                changeLibSelection();
            }
        });

        downButton = new Button(libsControlGroup, SWT.PUSH);
        downButton.setText(CoreMessages.dialog_edit_driver_button_down);
        downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downButton.setEnabled(false);
        downButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                DriverFileDescriptor selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex + 1);
                changeLibContent();
                changeLibSelection();
            }
        });

        Button cpButton = new Button(libsControlGroup, SWT.PUSH);
        cpButton.setText(CoreMessages.dialog_edit_driver_button_classpath);
        cpButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cpButton.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                ViewClasspathDialog cpDialog = new ViewClasspathDialog(getShell());
                cpDialog.open();
            }
        });

        changeLibContent();

        TabItem libsTab = new TabItem(group, SWT.NONE);
        libsTab.setText(CoreMessages.dialog_edit_driver_tab_name_driver_libraries);
        libsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_tooltip_driver_libraries);
        libsTab.setControl(libsGroup);
    }


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
        //Composite paramsGroup = new Composite(group, SWT.NONE);
        //paramsGroup.setLayout(new GridLayout(1, false));

        clientHomesPanel = new ClientHomesPanel(group, SWT.NONE);
        clientHomesPanel.loadHomes(driver);
        clientHomesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(CoreMessages.dialog_edit_driver_tab_name_client_homes);
        paramsTab.setToolTipText(CoreMessages.dialog_edit_driver_tab_name_client_homes);
        paramsTab.setControl(clientHomesPanel);
    }

    private void createLicenseTab(TabFolder group, String license)
    {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        Text licenseText = new Text(paramsGroup, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
        licenseText.setText(license);
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
        Button resetButton = createButton(parent, IDialogConstants.RETRY_ID, CoreMessages.dialog_edit_driver_button_reset_to_defaults, false);
        if (driver.isCustom()) {
            resetButton.setEnabled(false);
        }
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private DriverFileDescriptor getSelectedLibrary()
    {
        IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
        return selection == null || selection.isEmpty() ? null : (DriverFileDescriptor) selection.getFirstElement();
    }

    private void changeLibContent()
    {
        libTable.refresh();
        findClassButton.setEnabled(provider.isDriversManagable() && !libList.isEmpty());
    }

    private void changeLibSelection()
    {
        DriverFileDescriptor selectedLib = getSelectedLibrary();
        deleteButton.setEnabled(selectedLib != null);
        upButton.setEnabled(libList.indexOf(selectedLib) > 0);
        downButton.setEnabled(libList.indexOf(selectedLib) < libList.size() - 1);
    }

    private void onChangeProperty()
    {
        getButton(IDialogConstants.OK_ID).setEnabled(
            !CommonUtils.isEmpty(driverNameText.getText()) &&
                !CommonUtils.isEmpty(driverClassText.getText()));
    }

    private void resetSettings()
    {
        driverNameText.setText(CommonUtils.getString(driver.getOrigName()));
        driverDescText.setText(CommonUtils.getString(driver.getOrigDescription()));
        driverClassText.setText(CommonUtils.getString(driver.getOrigClassName()));
        driverURLText.setText(CommonUtils.getString(driver.getOrigSampleURL()));
        driverPortText.setText(driver.getOrigDefaultPort() == null ? "" : driver.getOrigDefaultPort().toString()); //$NON-NLS-1$
        if (!CommonUtils.isEmpty(driver.getCategory())) {
            driverCategoryCombo.setText(driver.getCategory());
        } else if (!CommonUtils.isEmpty(defaultCategory)) {
            driverCategoryCombo.setText(defaultCategory);
        }
//        anonymousCheck.setSelection(driver.isAnonymousAccess());
        libList.clear();
        for (DriverFileDescriptor lib : driver.getOrigFiles()) {
            if (lib.isDisabled()) {
                continue;
            }
            libList.add(lib);
        }
        changeLibContent();
        parametersEditor.loadProperties(driverPropertySource);
        connectionPropertiesEditor.loadProperties(connectionPropertySource);
        clientHomesPanel.loadHomes(driver);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.RETRY_ID) {
            resetSettings();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void okPressed()
    {
        // Set props
        driver.setName(driverNameText.getText());
        driver.setCategory(driverCategoryCombo.getText());
        driver.setDescription(CommonUtils.getString(driverDescText.getText()));
        driver.setDriverClassName(driverClassText.getText());
        driver.setSampleURL(driverURLText.getText());
        driver.setDriverDefaultPort(driverPortText.getText());
//        driver.setAnonymousAccess(anonymousCheck.getSelection());
        driver.setModified(true);

        // Set libraries
        for (DriverFileDescriptor lib : libList) {
            driver.addLibrary(lib);
        }
        for (DriverFileDescriptor lib : CommonUtils.copyList(driver.getFiles())) {
            if (lib.getType() == DBPDriverFileType.jar && !libList.contains(lib)) {
                driver.removeLibrary(lib);
            }
        }

        driver.setDriverParameters(driverPropertySource.getProperties());
        driver.setConnectionProperties(connectionPropertySource.getProperties());

        // Store client homes
        if (clientHomesPanel != null) {
            driver.setClientHomeIds(clientHomesPanel.getHomeIds());
        }

        // Finish
        if (provider.getDriver(driver.getId()) == null) {
            provider.addDriver(driver);
        }
        provider.getRegistry().saveDrivers();

        try {
            driver.loadDriver(true);
        } catch (Throwable ex) {
            UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_edit_driver_dialog_driver_error_title, CoreMessages.dialog_edit_driver_dialog_driver_error_message, ex);
        }

        super.okPressed();
    }

    private class ClassFindJob implements IRunnableWithProgress {

        private java.util.List<String> driverClassNames = new ArrayList<String>();

        private ClassFindJob() {
        }

        public java.util.List<String> getDriverClassNames() {
            return driverClassNames;
        }

        @Override
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            findDriverClasses(monitor);
        }

        private void findDriverClasses(IProgressMonitor monitor)
        {
            java.util.List<File> libFiles = new ArrayList<File>();
            java.util.List<URL> libURLs = new ArrayList<URL>();
            for (DriverFileDescriptor lib : libList) {
                File libFile = lib.getFile();
                if (libFile.exists() && !libFile.isDirectory() && lib.getType() == DBPDriverFileType.jar) {
                    libFiles.add(libFile);
                    try {
                        libURLs.add(libFile.toURI().toURL());
                    } catch (MalformedURLException e) {
                        log.debug(e);
                    }
                }
            }
            ClassLoader findCL = new URLClassLoader(libURLs.toArray(new URL[libURLs.size()]));

            for (File libFile : libFiles) {
                if (monitor.isCanceled()) {
                    break;
                }
                findDriverClasses(monitor, findCL, libFile);
            }
        }

        private void findDriverClasses(IProgressMonitor monitor, ClassLoader findCL, File libFile)
        {
            try {
                JarFile currentFile = new JarFile(libFile, false);
                monitor.beginTask(libFile.getName(), currentFile.size());

                for (Enumeration<?> e = currentFile.entries(); e.hasMoreElements(); ) {
                    {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        JarEntry current = (JarEntry) e.nextElement();
                        String fileName = current.getName();
                        if (fileName.endsWith(".class") && fileName.indexOf("$") == -1) { //$NON-NLS-1$ //$NON-NLS-2$
                            String className = fileName.replaceAll("/", ".").replace(".class", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            monitor.subTask(className);
                            try {
                                Class<?> aClass = Class.forName(className, false, findCL);
                                final int modifiers = aClass.getModifiers();
                                if (java.sql.Driver.class.isAssignableFrom(aClass) &&
                                    !Modifier.isAbstract(modifiers) &&
                                    !Modifier.isStatic(modifiers) &&
                                    Modifier.isPublic(modifiers))
                                {
                                    driverClassNames.add(aClass.getName());
                                }
                            } catch (Throwable e1) {
                                // do nothing
                            }
                            monitor.worked(1);
                        }
                    }
                }
                monitor.done();
            } catch (IOException e) {
                log.debug(e);
            }
        }

    }

}
