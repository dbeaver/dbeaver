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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverClassFindJob;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryAbstract;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesPanel;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * DriverEditDialog
 */
public class DriverEditDialog extends HelpEnabledDialog {
    private static final Log log = Log.getLog(DriverEditDialog.class);

    private static final String DIALOG_ID = "DBeaver.DriverEditDialog";//$NON-NLS-1$

    private static int dialogCount;

    private final boolean newDriver;
    private DataSourceProviderDescriptor provider;
    private DriverDescriptor driver;

    private String defaultCategory;
    private String curFolder = null;
    private TreeViewer libTable;
    private Button deleteButton;
    private Button updateVersionButton;
    private Button detailsButton;
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
    private final List<DBPDriverLibrary> origLibList;
    private PropertySourceCustom driverPropertySource;
    private PropertySourceCustom connectionPropertySource;
    private ClientHomesPanel clientHomesPanel;
    private Button embeddedDriverCheck;
    private Button anonymousDriverCheck;
    private Button allowsEmptyPasswordCheck;
    private Button nonInstantiableCheck;

    private boolean showAddFiles = false;

    static int getDialogCount() {
        return dialogCount;
    }

    public DriverEditDialog(Shell shell, DBPDriver driver) {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.driver = (DriverDescriptor) driver;
        this.provider = this.driver.getProviderDescriptor();
        this.defaultCategory = driver.getCategory();
        this.newDriver = false;
        this.origLibList = new ArrayList<>(driver.getDriverLibraries());
    }

    DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider, String category) {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.provider = provider;
        this.driver = provider.createDriver();
        this.defaultCategory = category;
        this.newDriver = true;
        this.origLibList = new ArrayList<>();
    }

    DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider, DriverDescriptor driver) {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.provider = provider;
        this.driver = provider.createDriver(driver);
        this.driver.setName(this.driver.getName() + " Copy");
        this.driver.setModified(true);

        // Mark driver and all its libraries as custom (#3867)
        this.driver.setCustom(true);
        for (DBPDriverLibrary library : this.driver.getDriverLibraries()) {
            if (library instanceof DriverLibraryAbstract) {
                ((DriverLibraryAbstract) library).setCustom(true);
            }
        }

        this.defaultCategory = driver.getCategory();
        this.newDriver = true;
        this.origLibList = new ArrayList<>();
    }

    public DriverDescriptor getDriver() {
        return driver;
    }

    public int open(boolean addFiles) {
        this.showAddFiles = addFiles;
        return open();
    }

    @Override
    public int open() {
        try {
            dialogCount++;
            return super.open();
        } finally {
            dialogCount--;
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ctl = super.createContents(parent);
        onChangeProperty();
        return ctl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        if (newDriver) {
            getShell().setText(UIConnectionMessages.dialog_edit_driver_title_create_driver);
        } else {
            getShell().setText(UIConnectionMessages.dialog_edit_driver_title_edit_driver + driver.getName() + "'"); //$NON-NLS-2$
            getShell().setImage(DBeaverIcons.getImage(driver.getPlainIcon()));
        }

        boolean isReadOnly = !provider.isDriversManagable();
        int advStyle = isReadOnly ? SWT.READ_ONLY : SWT.NONE;

        final Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        group.setLayoutData(gd);

        {
            Group propsGroup = UIUtils.createControlGroup(group, UIConnectionMessages.dialog_edit_driver_setting, 4, -1, -1);
            propsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            gd = new GridData(GridData.FILL_HORIZONTAL);
            driverNameText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_driver_name, driver.getName(), SWT.BORDER | advStyle, gd);
            driverNameText.setEnabled(driver == null || driver.isCustom());
            driverNameText.addModifyListener(e -> onChangeProperty());

            UIUtils.createControlLabel(propsGroup, UIConnectionMessages.dialog_edit_driver_type_label);
            final CSmartCombo<DataSourceProviderDescriptor> providerCombo = new CSmartCombo<>(propsGroup, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN, new LabelProvider() {
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DataSourceProviderDescriptor) element).getIcon());
                }

                @Override
                public String getText(Object element) {
                    return ((DataSourceProviderDescriptor) element).getName();
                }
            });
            providerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (newDriver) {
                for (DataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
                    if (provider.isDriversManagable()) {
                        providerCombo.addItem(provider);
                    }
                }
                providerCombo.select(provider);
                providerCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        provider = providerCombo.getItem(providerCombo.getSelectionIndex());
                        driver = provider.createDriver();
                    }
                });
            } else {
                providerCombo.addItem(provider);
                providerCombo.select(provider);
            }

            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            driverClassText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_class_name, CommonUtils.notEmpty(driver.getDriverClassName()), SWT.BORDER | advStyle, gd);
            driverClassText.setToolTipText(UIConnectionMessages.dialog_edit_driver_label_class_name_tip);
            driverClassText.addModifyListener(e -> onChangeProperty());

            driverURLText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_sample_url, CommonUtils.notEmpty(driver.getSampleURL()), SWT.BORDER | advStyle, gd);
            driverURLText.setToolTipText(UIConnectionMessages.dialog_edit_driver_label_sample_url_tip);
            driverURLText.addModifyListener(e -> onChangeProperty());
            driverURLText.setEnabled(driver == null || driver.isUseURL());

            UIUtils.createControlLabel(propsGroup, UIConnectionMessages.dialog_edit_driver_label_default_port);
            driverPortText = new Text(propsGroup, SWT.BORDER | advStyle);
            driverPortText.setText(driver.getDefaultPort() == null ? "" : driver.getDefaultPort());
            driverPortText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            driverPortText.addModifyListener(e -> onChangeProperty());
            UIUtils.createEmptyLabel(propsGroup, 2, 1);

            Composite optionsPanel = new Composite(propsGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            optionsPanel.setLayoutData(gd);
            optionsPanel.setLayout(new RowLayout());
            embeddedDriverCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_embedded_label, UIConnectionMessages.dialog_edit_driver_embedded_tip, driver.isEmbedded(), 1);
            anonymousDriverCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_anonymous_label, UIConnectionMessages.dialog_edit_driver_anonymous_tip, driver.isAnonymousAccess(), 1);
            allowsEmptyPasswordCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_aloows_empty_password_label, UIConnectionMessages.dialog_edit_driver_aloows_empty_password_tip, driver.isAnonymousAccess(), 1);
            nonInstantiableCheck = UIUtils.createCheckbox(optionsPanel, "Use legacy JDBC instantiation", "Do not instantiate driver directly, use DriverManager always.", !driver.isInstantiable(), 1);

            if (isReadOnly) {
                embeddedDriverCheck.setEnabled(false);
                anonymousDriverCheck.setEnabled(false);
                allowsEmptyPasswordCheck.setEnabled(false);
                nonInstantiableCheck.setEnabled(false);
            }
        }

        {
            Group infoGroup = UIUtils.createControlGroup(group, UIConnectionMessages.dialog_edit_driver_description, 4, -1, -1);
            infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            driverCategoryCombo = UIUtils.createLabelCombo(infoGroup, UIConnectionMessages.dialog_edit_driver_label_category, SWT.BORDER | SWT.DROP_DOWN | advStyle);
            driverCategoryCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            if (isReadOnly) {
                driverCategoryCombo.setEnabled(false);
            }
            Set<String> categories = new TreeSet<>();
            for (DataSourceProviderDescriptor provider : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
                for (DriverDescriptor drv : provider.getEnabledDrivers()) {
                    if (!CommonUtils.isEmpty(drv.getCategory())) {
                        categories.add(drv.getCategory());
                    }
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

            {
                gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
                Text idText = UIUtils.createLabelText(infoGroup, "ID", driver.getId(), SWT.BORDER | SWT.READ_ONLY, gd);
                idText.setToolTipText("Internal driver ID.\nCan be used in connections configurations and CLI commands.");
            }

            driverDescText = UIUtils.createLabelText(infoGroup, UIConnectionMessages.dialog_edit_driver_label_description, CommonUtils.notEmpty(driver.getDescription()), SWT.BORDER | advStyle);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            driverDescText.setLayoutData(gd);

            if (!CommonUtils.isEmpty(driver.getWebURL())) {
                UIUtils.createControlLabel(infoGroup, UIConnectionMessages.dialog_edit_driver_label_website);
                Link urlLabel = UIUtils.createLink(infoGroup, "<a>" + driver.getWebURL() + "</a>", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        UIUtils.launchProgram(driver.getWebURL());
                    }
                });
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 3;
                driverDescText.setLayoutData(gd);
                urlLabel.setLayoutData(gd);
            }
        }

        {
            TabFolder tabFolder = new TabFolder(group, SWT.NONE);
            tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

            createLibrariesTab(tabFolder);
            createConnectionPropertiesTab(tabFolder);
            createParametersTab(tabFolder);
            // Client homes
            if (driver.getNativeClientManager() != null) {
                createClientHomesTab(tabFolder);
            }

            final String license = driver.getLicense();
            if (license != null) {
                createLicenseTab(tabFolder, license);
            }

            tabFolder.setSelection(0);
        }

        loadSettings(false);

        if (showAddFiles) {
            getShell().getDisplay().asyncExec(this::addLibraryFiles);
        }

        driverNameText.setFocus();

        return group;
    }

    private void createLibrariesTab(TabFolder group) {
        GridData gd;
        Composite libsGroup = new Composite(group, SWT.NONE);
        libsGroup.setLayout(new GridLayout(2, false));

        {
            Composite libsListGroup = new Composite(libsGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_BOTH);
            libsListGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            libsListGroup.setLayout(layout);

            // Additional libraries list
            libTable = new TreeViewer(libsListGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            libTable.setContentProvider(new LibContentProvider());
            libTable.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    final Object element = cell.getElement();
                    if (element instanceof DBPDriverLibrary) {
                        DBPDriverLibrary lib = (DBPDriverLibrary) element;
                        String displayName = lib.getDisplayName();
                        if (lib.getPreferredVersion() != null) {
                            displayName += " [" + lib.getPreferredVersion() + "]";
                        }
                        cell.setText(displayName);
                        File localFile = lib.getLocalFile();
                        if (localFile != null && !localFile.exists()) {
                            cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                        } else if (!driver.isLibraryResolved(lib)) {
                            cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
                        } else {
                            cell.setForeground(null);
                        }
                        cell.setImage(DBeaverIcons.getImage(lib.getIcon()));
                    } else {
                        cell.setText(element.toString());
                        cell.setImage(DBeaverIcons.getImage(DBIcon.JAR));
                    }
                }

                @Override
                public String getToolTipText(Object element) {
                    if (element instanceof DBPDriverLibrary) {
                        File localFile = ((DBPDriverLibrary) element).getLocalFile();
                        return localFile == null ? "N/A" : localFile.getAbsolutePath();
                    }
                    return super.getToolTipText(element);
                }
            });
            ColumnViewerToolTipSupport.enableFor(libTable);
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, event -> changeLibSelection());
            libTable.addDoubleClickListener(event -> {
                if (getSelectedLibrary() instanceof DriverLibraryMavenArtifact) {
                    editMavenArtifact();
                }
            });

            // Find driver class
            boolean isReadOnly = !provider.isDriversManagable();

            Composite findClassGroup = new Composite(libsListGroup, SWT.TOP);
            findClassGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            findClassGroup.setLayout(layout);

            UIUtils.createControlLabel(findClassGroup, UIConnectionMessages.dialog_edit_driver_label_driver_class);
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
            findClassButton.setText(UIConnectionMessages.dialog_edit_driver_button_bind_class);
            findClassButton.addListener(SWT.Selection, event -> {
                try {
                    DriverClassFindJob classFinder = new DriverClassFindJob(driver, java.sql.Driver.class.getName(), true);
                    UIUtils.runInProgressDialog(classFinder);

                    if (classListCombo != null && !classListCombo.isDisposed()) {
                        List<String> classNames = classFinder.getDriverClassNames();
                        classListCombo.setItems(classNames.toArray(new String[classNames.size()]));
                        classListCombo.setListVisible(true);
                    }

                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                }
            });
            findClassButton.setEnabled(!isReadOnly);
        }

        Composite libsControlGroup = new Composite(libsGroup, SWT.TOP);
        libsControlGroup.setLayout(new GridLayout(1, true));
        libsControlGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_add_file, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addLibraryFiles();
            }
        });

        UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_add_folder, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addLibraryFolder();
            }
        });

        UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_add_artifact, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addMavenArtifact();
            }
        });

        updateVersionButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_update_version, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                driver.updateFiles();
                changeLibContent();
            }
        });

        detailsButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_details, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new DriverLibraryDetailsDialog(getShell(), driver, getSelectedLibrary()).open();
            }
        });
        detailsButton.setEnabled(false);
        deleteButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_delete, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
                if (selection != null && !selection.isEmpty()) {
                    if (UIUtils.confirmAction(getShell(), "Delete library", "Are you sure you want to delete selected libraries?")) {
                        for (Object obj : selection.toArray()) {
                            if (obj instanceof DriverLibraryAbstract) {
                                driver.removeDriverLibrary((DriverLibraryAbstract) obj);
                            }
                        }
                    }
                }
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

        UIUtils.createHorizontalLine(libsControlGroup);

        UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_classpath, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ViewClasspathDialog cpDialog = new ViewClasspathDialog(getShell());
                cpDialog.open();
            }
        });

        changeLibContent();

        TabItem libsTab = new TabItem(group, SWT.NONE);
        libsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_driver_libraries);
        libsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_driver_libraries);
        libsTab.setControl(libsGroup);
    }

    private void addMavenArtifact() {
        EditMavenArtifactDialog fd = new EditMavenArtifactDialog(getShell(), driver, null);
        if (fd.open() == IDialogConstants.OK_ID) {
            driver.addDriverLibrary(fd.getLibrary(), true);
            changeLibContent();
        }
    }

    private void editMavenArtifact() {
        DriverLibraryAbstract selectedLibrary = getSelectedLibrary();
        if (selectedLibrary instanceof DriverLibraryMavenArtifact) {
            EditMavenArtifactDialog fd = new EditMavenArtifactDialog(getShell(), driver, (DriverLibraryMavenArtifact) selectedLibrary);
            if (fd.open() == IDialogConstants.OK_ID) {
                libTable.refresh();
            }
        }
    }

    private void addLibraryFolder() {
        DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.MULTI);
        fd.setText(UIConnectionMessages.dialog_edit_driver_dialog_open_driver_directory);
        fd.setFilterPath(curFolder);
        String selected = fd.open();
        if (selected != null) {
            curFolder = fd.getFilterPath();
            driver.addDriverLibrary(DriverLibraryAbstract.createFromPath(
                driver,
                DBPDriverLibrary.FileType.jar,
                selected,
                null), true);
            changeLibContent();
        }
    }

    private void addLibraryFiles() {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
        fd.setText(UIConnectionMessages.dialog_edit_driver_dialog_open_driver_library);
        fd.setFilterPath(curFolder);
        String[] filterExt = {"*.jar;*.zip", "*.dll;*.so", "*", "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
        fd.setFilterExtensions(filterExt);
        String selected = fd.open();
        if (selected != null) {
            curFolder = fd.getFilterPath();
            String[] fileNames = fd.getFileNames();
            if (!ArrayUtils.isEmpty(fileNames)) {
                File folderFile = new File(curFolder);
                for (String fileName : fileNames) {
                    driver.addDriverLibrary(
                        DriverLibraryAbstract.createFromPath(
                            driver,
                            fileName.endsWith(".jar") || fileName.endsWith(".zip") ? DBPDriverLibrary.FileType.jar : DBPDriverLibrary.FileType.lib,
                            new File(folderFile, fileName).getAbsolutePath(),
                            null), true);
                }
                changeLibContent();
            }
        }
    }

    private void createParametersTab(TabFolder group) {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        parametersEditor = new PropertyTreeViewer(paramsGroup, SWT.BORDER);
        driverPropertySource = new PropertySourceCustom(
            driver.getProviderDescriptor().getDriverProperties(),
            driver.getDriverParameters());
        driverPropertySource.addDefaultValues(driver.getDefaultDriverParameters());
        parametersEditor.loadProperties(driverPropertySource);

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_advanced_parameters);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_advanced_parameters);
        paramsTab.setControl(paramsGroup);
    }

    private void createConnectionPropertiesTab(TabFolder group) {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        connectionPropertiesEditor = new ConnectionPropertiesControl(paramsGroup, SWT.BORDER);
        connectionPropertySource = connectionPropertiesEditor.makeProperties(driver, driver.getConnectionProperties());
        connectionPropertiesEditor.loadProperties(connectionPropertySource);


        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_connection_properties);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_connection_properties);
        paramsTab.setControl(paramsGroup);
    }

    private void createClientHomesTab(TabFolder group) {
        clientHomesPanel = new ClientHomesPanel(group, SWT.NONE);
        clientHomesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_client_homes);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_name_client_homes);
        paramsTab.setControl(clientHomesPanel);
        group.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item == paramsTab) {
                    clientHomesPanel.loadHomes(driver);
                    group.removeSelectionListener(this);
                }
            }
        });
    }

    private void createLicenseTab(TabFolder group, String license) {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        Text licenseText = new Text(paramsGroup, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
        licenseText.setText(license);
        licenseText.setEditable(false);
        licenseText.setMessage(UIConnectionMessages.dialog_edit_driver_text_driver_license);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 200;
        //gd.grabExcessVerticalSpace = true;
        licenseText.setLayoutData(gd);

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_license);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_license);
        paramsTab.setControl(paramsGroup);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button resetButton = createButton(parent, IDialogConstants.RETRY_ID, UIMessages.button_reset_to_defaults, false);
        if (driver.isCustom()) {
            resetButton.setEnabled(false);
        }
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private DriverLibraryAbstract getSelectedLibrary() {
        IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
        if (selection != null && !selection.isEmpty()) {
            final Object element = selection.getFirstElement();
            if (element instanceof DriverLibraryAbstract) {
                return (DriverLibraryAbstract) element;
            }
        }
        return null;
    }

    private void changeLibContent() {
        libTable.setInput(driver.getEnabledDriverLibraries());
        boolean hasFiles = false, hasDownloads = false;
        for (DBPDriverLibrary library : driver.getDriverLibraries()) {
            final File localFile = library.getLocalFile();
            hasFiles = hasFiles || (!library.isDisabled() && localFile != null && localFile.exists());
            if (!hasFiles && !library.isDisabled()) {
                final Collection<DriverDescriptor.DriverFileInfo> files = driver.getLibraryFiles(library);
                if (files != null) {
                    for (DriverDescriptor.DriverFileInfo file : files) {
                        if (file.getFile() != null && file.getFile().exists()) {
                            hasFiles = true;
                        }
                    }
                }
            }

            if (!library.isDisabled() && library.isDownloadable()) {
                hasDownloads = true;
            }
        }
        findClassButton.setEnabled(provider.isDriversManagable() && hasFiles);
        updateVersionButton.setEnabled(hasDownloads);
    }

    private void changeLibSelection() {
        DriverLibraryAbstract selectedLib = getSelectedLibrary();
        detailsButton.setEnabled(selectedLib != null);
        deleteButton.setEnabled(selectedLib != null);
/*
        upButton.setEnabled(libList.indexOf(selectedLib) > 0);
        downButton.setEnabled(libList.indexOf(selectedLib) < libList.size() - 1);
*/
    }

    private void onChangeProperty() {
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            boolean isValid = !CommonUtils.isEmpty(driverNameText.getText());
            button.setEnabled(isValid);
        }
    }

    private void loadSettings(boolean original) {
        driverNameText.setText(original ? CommonUtils.notEmpty(driver.getOrigName()) : CommonUtils.notEmpty(driver.getName()));
        driverDescText.setText(original ? CommonUtils.notEmpty(driver.getOrigDescription()) : CommonUtils.notEmpty(driver.getDescription()));
        driverClassText.setText(original ? CommonUtils.notEmpty(driver.getOrigClassName()) : CommonUtils.notEmpty(driver.getDriverClassName()));
        driverURLText.setText(original ? CommonUtils.notEmpty(driver.getOrigSampleURL()) : CommonUtils.notEmpty(driver.getSampleURL()));
        driverPortText.setText(original ?
            (driver.getOrigDefaultPort() == null ? "" : driver.getOrigDefaultPort()) : //$NON-NLS-1$
            (driver.getDefaultPort() == null ? "" : driver.getDefaultPort())); //$NON-NLS-1$

        embeddedDriverCheck.setSelection(original ? driver.isOrigEmbedded() : driver.isEmbedded());
        anonymousDriverCheck.setSelection(original ? driver.isOrigAnonymousAccess() : driver.isAnonymousAccess());
        allowsEmptyPasswordCheck.setSelection(original ? driver.isOrigAllowsEmptyPassword() : driver.isAllowsEmptyPassword());
        nonInstantiableCheck.setSelection(original ? !driver.isOrigInstantiable() : !driver.isInstantiable());

        if (original) {
            resetLibraries(true);
        }
        if (libTable != null) {
            libTable.setInput(driver.getEnabledDriverLibraries());
            changeLibContent();
            changeLibSelection();
        }

        parametersEditor.loadProperties(driverPropertySource);
        connectionPropertiesEditor.loadProperties(connectionPropertySource);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.RETRY_ID) {
            loadSettings(true);
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void cancelPressed() {
        resetLibraries(false);

        super.cancelPressed();
    }

    private void resetLibraries(boolean original) {
        // Set libraries
        Collection<DBPDriverLibrary> newLibList = CommonUtils.copyList(original ? driver.getOrigFiles() : origLibList);
        for (DBPDriverLibrary lib : newLibList) {
            lib.setDisabled(false);
            driver.addDriverLibrary(lib, true);
        }
        for (DBPDriverLibrary lib : CommonUtils.copyList(driver.getDriverLibraries())) {
            if (!newLibList.contains(lib)) {
                driver.removeDriverLibrary(lib);
            }
        }
    }

    @Override
    protected void okPressed() {

        // Set props
        driver.setName(driverNameText.getText());
        driver.setCategory(driverCategoryCombo.getText());
        driver.setDescription(CommonUtils.notEmpty(driverDescText.getText()));
        driver.setDriverClassName(driverClassText.getText());
        driver.setSampleURL(driverURLText.getText());
        driver.setDriverDefaultPort(driverPortText.getText());
        driver.setEmbedded(embeddedDriverCheck.getSelection());
        driver.setAnonymousAccess(anonymousDriverCheck.getSelection());
        driver.setAllowsEmptyPassword(allowsEmptyPasswordCheck.getSelection());
        driver.setInstantiable(!nonInstantiableCheck.getSelection());

//        driver.setAnonymousAccess(anonymousCheck.getSelection());
        driver.setModified(true);

        driver.setDriverParameters(CommonUtils.makeStringMap(driverPropertySource.getPropertiesWithDefaults()));
        driver.setConnectionProperties(CommonUtils.makeStringMap(connectionPropertySource.getProperties()));

        // Store client homes
        if (clientHomesPanel != null) {
            driver.setNativeClientLocations(clientHomesPanel.getLocalLocations());
        }

        DriverDescriptor oldDriver = provider.getDriverByName(driver.getCategory(), driver.getName());
        if (oldDriver != null && oldDriver != driver && !oldDriver.isDisabled() && oldDriver.getReplacedBy() == null) {
            UIUtils.showMessageBox(getShell(), "Driver settings save", "Driver '" + driver.getName() + "' already exists. Change driver name", SWT.ICON_ERROR);
            return;
        }

        // Finish
        if (provider.getDriver(driver.getId()) == null) {
            provider.addDriver(driver);
        }
        provider.getRegistry().saveDrivers();

        super.okPressed();
    }

    public static void showBadConfigDialog(final Shell shell, final String message, final DBException error) {
        //log.debug(message);
        Runnable runnable = () -> {
            DBPDataSource dataSource = error.getDataSource();
            String title = "Bad driver [" + dataSource.getContainer().getDriver().getName() + "] configuration";
            new BadDriverConfigDialog(shell, title, message == null ? title : message, error).open();
        };
        UIUtils.syncExec(runnable);
    }

    private static class BadDriverConfigDialog extends StandardErrorDialog {

        private final DBPDataSource dataSource;

        BadDriverConfigDialog(Shell shell, String title, String message, DBException error) {
            super(
                shell == null ? UIUtils.getActiveWorkbenchShell() : shell,
                title,
                message,
                RuntimeUtils.stripStack(GeneralUtils.makeExceptionStatus(error)),
                IStatus.ERROR);
            dataSource = error.getDataSource();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.RETRY_ID, "Open Driver &Configuration", true);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int id) {
            if (id == IDialogConstants.RETRY_ID) {
                UIUtils.asyncExec(() -> {
                    DriverEditDialog dialog = new DriverEditDialog(
                        UIUtils.getActiveWorkbenchShell(),
                        dataSource.getContainer().getDriver());
                    dialog.open();
                });
                super.buttonPressed(IDialogConstants.OK_ID);
            }
            super.buttonPressed(id);
        }
    }

    private class LibContentProvider implements ITreeContentProvider {
        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Collection) {
                return ((Collection<?>) inputElement).toArray();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof DBPDriverLibrary) {
                final Collection<DriverDescriptor.DriverFileInfo> files = driver.getLibraryFiles((DBPDriverLibrary) parentElement);
                if (CommonUtils.isEmpty(files)) {
                    return null;
                }
                return files.toArray(new Object[files.size()]);
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return element instanceof DBPDriverLibrary &&
                !CommonUtils.isEmpty(driver.getLibraryFiles((DBPDriverLibrary) element));
        }
    }
}
