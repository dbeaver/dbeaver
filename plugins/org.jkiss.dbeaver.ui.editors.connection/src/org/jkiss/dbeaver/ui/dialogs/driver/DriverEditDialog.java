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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesPanel;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.preferences.PrefPageDriversClasspath;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

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

    private String curFolder = null;
    private TreeViewer libTable;
    private Button editButton;
    private Button deleteButton;
    private Button updateVersionButton;
    private Button detailsButton;
    private Combo classListCombo;
    private Button findClassButton;
    private Text driverNameText;
    private Text driverDescText;
    private Text driverClassText;
    private Text driverURLText;
    private Text driverPortText;
    private Text driverUserText;
    private Text driverDatabaseText;
    private PropertyTreeViewer parametersEditor;
    private ConnectionPropertiesControl connectionPropertiesEditor;
    private PropertySourceCustom driverPropertySource;
    private PropertySourceCustom connectionPropertySource;
    private ClientHomesPanel clientHomesPanel;
    private Button embeddedDriverCheck;
    private Button anonymousDriverCheck;
    private Button allowsEmptyPasswordCheck;
    private Button nonInstantiableCheck;
    private Button propagateDriverPropertiesCheck;
    private Button threadSafeCheck;

    private boolean showAddFiles = false;

    private final List<DBPDriverLibrary> libraries = new ArrayList<>();

    static int getDialogCount() {
        return dialogCount;
    }

    public DriverEditDialog(Shell shell, DBPDriver driver) {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.driver = (DriverDescriptor) driver;
        this.provider = this.driver.getProviderDescriptor();
        this.newDriver = false;
    }

    DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider, String category) {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.provider = provider;
        this.driver = provider.createDriver();
        this.newDriver = true;
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

        this.newDriver = true;
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
    protected Composite createDialogArea(Composite parent) {
        if (newDriver) {
            getShell().setText(UIConnectionMessages.dialog_edit_driver_title_create_driver);
        } else {
            getShell().setText(UIConnectionMessages.dialog_edit_driver_title_edit_driver + driver.getName() + "'"); //$NON-NLS-2$
            getShell().setImage(DBeaverIcons.getImage(driver.getPlainIcon()));
        }

        final Composite group = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 700;
        group.setLayoutData(gd);

        {
            CTabFolder tabFolder = new CTabFolder(group, SWT.FLAT);
            tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

            createMainTab(tabFolder);
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

    private void createMainTab(CTabFolder group) {
        boolean isReadOnly = !provider.isDriversManagable();
        int advStyle = isReadOnly ? SWT.READ_ONLY : SWT.NONE;

        Composite propsGroup = new Composite(group, SWT.NONE);
        propsGroup.setLayout(new GridLayout(4, false));
        propsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = UIUtils.getFontHeight(propsGroup) * 20;
        driverNameText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_driver_name, driver.getName(), SWT.BORDER | advStyle, gd);
        driverNameText.setEnabled(driver == null || driver.isCustom());
        driverNameText.addModifyListener(e -> onChangeProperty());

        {
            Composite driverTypeGroup = UIUtils.createComposite(propsGroup, 2);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            driverTypeGroup.setLayoutData(gd);
            UIUtils.createControlLabel(driverTypeGroup, UIConnectionMessages.dialog_edit_driver_type_label);
            final CSmartCombo<DataSourceProviderDescriptor> providerCombo = new CSmartCombo<>(
                driverTypeGroup,
                SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN,
                new LabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return DBeaverIcons.getImage(((DataSourceProviderDescriptor) element).getIcon());
                    }

                    @Override
                    public String getText(Object element) {
                        return ((DataSourceProviderDescriptor) element).getName();
                    }
                });
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = UIUtils.getFontHeight(propsGroup) * 20;
            providerCombo.setLayoutData(gd);
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
        }

        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        driverClassText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_class_name, CommonUtils.notEmpty(driver.getDriverClassName()), SWT.BORDER | advStyle, gd);
        driverClassText.setToolTipText(UIConnectionMessages.dialog_edit_driver_label_class_name_tip);
        driverClassText.addModifyListener(e -> onChangeProperty());

        driverURLText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_sample_url, CommonUtils.notEmpty(driver.getSampleURL()), SWT.BORDER | advStyle, gd);
        driverURLText.setToolTipText(UIConnectionMessages.dialog_edit_driver_label_sample_url_tip);
        driverURLText.addModifyListener(e -> onChangeProperty());
        driverURLText.setEnabled(driver == null || driver.isSampleURLApplicable());

        driverPortText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_default_port, CommonUtils.notEmpty(driver.getDefaultPort()), SWT.BORDER | advStyle);
        driverPortText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        driverPortText.addModifyListener(e -> onChangeProperty());

        driverDatabaseText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_default_database, CommonUtils.notEmpty(driver.getDefaultDatabase()), SWT.BORDER | advStyle);
        driverDatabaseText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        driverDatabaseText.addModifyListener(e -> onChangeProperty());

        driverUserText = UIUtils.createLabelText(propsGroup, UIConnectionMessages.dialog_edit_driver_label_default_user, CommonUtils.notEmpty(driver.getDefaultUser()), SWT.BORDER | advStyle);
        driverUserText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        driverUserText.addModifyListener(e -> onChangeProperty());

        UIUtils.createEmptyLabel(propsGroup, 2, 1);

        Composite optionsPanel = new Composite(propsGroup, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        optionsPanel.setLayoutData(gd);
        optionsPanel.setLayout(new RowLayout());
        embeddedDriverCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_embedded_label, UIConnectionMessages.dialog_edit_driver_embedded_tip, driver.isEmbedded(), 1);
        propagateDriverPropertiesCheck = UIUtils.createCheckbox(
            optionsPanel,
            UIConnectionMessages.dialog_edit_driver_propagate_driver_properties_label,
            UIConnectionMessages.dialog_edit_driver_propagate_driver_properties_tip,
            driver.isPropagateDriverProperties(),
            1
        );
        anonymousDriverCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_anonymous_label, UIConnectionMessages.dialog_edit_driver_anonymous_tip, driver.isAnonymousAccess(), 1);
        allowsEmptyPasswordCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_allows_empty_password_label, UIConnectionMessages.dialog_edit_driver_allows_empty_password_tip, driver.isAnonymousAccess(), 1);
        nonInstantiableCheck = UIUtils.createCheckbox(optionsPanel, UIConnectionMessages.dialog_edit_driver_use_legacy_instantiation_label, UIConnectionMessages.dialog_edit_driver_use_legacy_instantiation_tip, !driver.isInstantiable(), 1);
        threadSafeCheck = UIUtils.createCheckbox(optionsPanel, "Thread safe driver", "Driver is thread safe (default). Otherwise DBeaver will lock all driver invocations to protect it from any data corruptions.", driver.isThreadSafeDriver(), 1);

        if (isReadOnly) {
            embeddedDriverCheck.setEnabled(false);
            anonymousDriverCheck.setEnabled(false);
            allowsEmptyPasswordCheck.setEnabled(false);
            nonInstantiableCheck.setEnabled(false);
            threadSafeCheck.setEnabled(false);
        }

        Group infoGroup = UIUtils.createControlGroup(propsGroup, UIConnectionMessages.dialog_edit_driver_description, 4, -1, -1);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        infoGroup.setLayoutData(gd);

        {
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            Text idText = UIUtils.createLabelText(infoGroup, UIConnectionMessages.dialog_edit_driver_label_id, driver.getId(), SWT.BORDER | SWT.READ_ONLY, gd);
            idText.setToolTipText(UIConnectionMessages.dialog_edit_driver_label_id_tip);
        }

        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.heightHint = 3 * UIUtils.getFontHeight(infoGroup);
        driverDescText = UIUtils.createLabelText(
            infoGroup,
            UIConnectionMessages.dialog_edit_driver_label_description,
            CommonUtils.notEmpty(driver.getDescription()),
            SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | advStyle,
            gd);

        if (!CommonUtils.isEmpty(driver.getWebURL())) {
            UIUtils.createControlLabel(infoGroup, UIConnectionMessages.dialog_edit_driver_label_website);
            Link urlLabel = UIUtils.createLink(infoGroup, "<a>" + driver.getWebURL() + "</a>", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(driver.getWebURL());
                }
            });
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            driverDescText.setLayoutData(gd);
            urlLabel.setLayoutData(gd);
        }

        CTabItem paramsTab = new CTabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_setting);
        paramsTab.setControl(propsGroup);

        UIUtils.asyncExec(() -> propsGroup.layout(true, true));
    }

    private void createLibrariesTab(CTabFolder group) {
        libraries.addAll(driver.getEnabledDriverLibraries());

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
                    if (element instanceof DBPDriverLibrary lib) {
                        String displayName = lib.getDisplayName();
                        if (lib.getPreferredVersion() != null) {
                            displayName += " [" + lib.getPreferredVersion() + "]";
                        }
                        cell.setText(displayName);
                        Path localFile = lib.getLocalFile();
                        if (localFile != null && !Files.exists(localFile)) {
                            cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                        } else if (!driver.isLibraryResolved(lib)) {
                            cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
                        } else {
                            cell.setForeground(null);
                        }
                        cell.setImage(DBeaverIcons.getImage(lib.getIcon()));
                    } else {
                        cell.setText(element.toString());
                        if (element instanceof DriverDescriptor.DriverFileInfo) {
                            if (((DriverDescriptor.DriverFileInfo)element).getType() == DBPDriverLibrary.FileType.license) {
                                cell.setImage(DBeaverIcons.getImage(DBIcon.TYPE_TEXT));
                            } else {
                                cell.setImage(DBeaverIcons.getImage(DBIcon.JAR));
                            }
                        } else {
                            cell.setImage(DBeaverIcons.getImage(DBIcon.JAR));
                        }
                    }
                }

                @Override
                public String getToolTipText(Object element) {
                    if (element instanceof DBPDriverLibrary dl) {
                        Path localFile = dl.getLocalFile();
                        return localFile == null ? "N/A" : localFile.toAbsolutePath().toString();
                    } else if (element instanceof DriverDescriptor.DriverFileInfo dfi) {
                        Path localFile = dfi.getFile();
                        return localFile == null ? "N/A" : localFile.toString();
                    }
                    return super.getToolTipText(element);
                }
            });
            ColumnViewerToolTipSupport.enableFor(libTable);
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, event -> changeLibSelection());
            libTable.addDoubleClickListener(event -> editSelectedLibrary());
            UIWidgets.setControlContextMenu(libTable.getTree(), manager ->
                UIWidgets.fillDefaultTreeContextMenu(manager, libTable.getTree()));

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
                    DriverDescriptor test = new DriverDescriptor(driver.getProviderDescriptor(), "test", driver);
                    saveDriverSettings(test);
                    DriverClassFindJob classFinder = new DriverClassFindJob(test, java.sql.Driver.class.getName(), true);
                    UIUtils.runInProgressDialog(classFinder);

                    if (classListCombo != null && !classListCombo.isDisposed()) {
                        List<String> classNames = classFinder.getDriverClassNames();
                        classListCombo.setItems(classNames.toArray(new String[0]));
                        if (!RuntimeUtils.isMacOS()) {
                            classListCombo.setListVisible(true);
                        } else if (!classNames.isEmpty()) {
                            classListCombo.setText(classNames.get(0));
                        }
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
        if (!DBWorkbench.isDistributed()) {
            UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_add_artifact, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    addMavenArtifact();
                }
            });

            editButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_driver_manager_button_edit, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editSelectedLibrary();
                }
            });
            editButton.setEnabled(false);
        }

        deleteButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_delete, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
                if (selection != null && !selection.isEmpty()) {
                    if (UIUtils.confirmAction(getShell(), UIConnectionMessages.dialog_edit_driver_dialog_delete_library_title, UIConnectionMessages.dialog_edit_driver_dialog_delete_library_message)) {
                        for (Object obj : selection.toArray()) {
                            if (obj instanceof DriverLibraryAbstract) {
                                driver.resetDriverInstance();
                                libraries.remove(obj);
                                changeLibContent();
                            }
                        }
                    }
                }
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

        UIUtils.createHorizontalLine(libsControlGroup);

        if (!DBWorkbench.isDistributed()) {
            updateVersionButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_update_version, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    driver.setDriverLibraries(libraries);
                    driver.updateFiles();
                    changeLibContent();
                }
            });
        }

        detailsButton = UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_details, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new DriverLibraryDetailsDialog(getShell(), driver, getSelectedLibrary()).open();
            }
        });
        detailsButton.setEnabled(false);

        UIUtils.createToolButton(libsControlGroup, UIConnectionMessages.dialog_edit_driver_button_classpath, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                UIUtils.showPreferencesFor(null, null, PrefPageDriversClasspath.PAGE_ID);
            }
        });
        UIUtils.createToolButton(libsControlGroup, ActionUtils.findCommandName(IWorkbenchCommandConstants.FILE_EXPORT), new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (DBPDriverLibrary lib : libraries) {
                    if (!driver.isLibraryResolved(lib)) {
                        if (!UIUtils.confirmAction(getShell(), "Not all files present",
                            "Driver files weren't downloaded. " +
                            "You need to click '" + UIConnectionMessages.dialog_edit_driver_button_update_version + "' before exporting.\n" +
                            "Are you sure you want to continue with incomplete files?")
                        ) {
                            return;
                        }
                        break;
                    }
                }
                DriverEditHelpers.exportDriverLibraries(getShell(), libraries);
            }
        });

        changeLibContent();

        CTabItem libsTab = new CTabItem(group, SWT.NONE);
        libsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_driver_libraries);
        libsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_driver_libraries);
        libsTab.setControl(libsGroup);
    }

    private void editSelectedLibrary() {
        final DriverLibraryAbstract selectedLibrary = getSelectedLibrary();
        if (selectedLibrary instanceof DriverLibraryMavenArtifact) {
            editMavenArtifact();
        } else if (selectedLibrary instanceof DriverLibraryLocal) {
            DriverEditHelpers.showFileInExplorer(selectedLibrary.getLocalFile());
        } else {
            IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
            if (!selection.isEmpty()) {
                Object element = selection.getFirstElement();
                if (element instanceof DriverDescriptor.DriverFileInfo dfi) {
                    DriverEditHelpers.showFileInExplorer(dfi.getFile());
                }
            }
        }
    }

    private void addMavenArtifact() {
        EditMavenArtifactDialog fd = new EditMavenArtifactDialog(getShell(), driver, null);
        if (fd.open() == IDialogConstants.OK_ID) {
            libraries.addAll(fd.getArtifacts());
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
            libraries.add(DriverLibraryAbstract.createFromPath(
                driver,
                DBPDriverLibrary.FileType.jar,
                selected,
                null));
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
                    libraries.add(
                        DriverLibraryAbstract.createFromPath(
                            driver,
                            DBPDriverLibrary.FileType.getFileTypeByFileName(fileName),
                            new File(folderFile, fileName).getAbsolutePath(),
                            null));
                }
                changeLibContent();
            }
        }
    }

    private void createParametersTab(CTabFolder group) {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        parametersEditor = new PropertyTreeViewer(paramsGroup, SWT.BORDER);
        driverPropertySource = new PropertySourceCustom(
            driver.getProviderDescriptor().getDriverProperties(),
            driver.getDriverParameters());
        driverPropertySource.addDefaultValues(driver.getDefaultDriverParameters());
        parametersEditor.loadProperties(driverPropertySource);

        CTabItem paramsTab = new CTabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_advanced_parameters);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_advanced_parameters);
        paramsTab.setControl(paramsGroup);
    }

    private void createConnectionPropertiesTab(CTabFolder group) {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        connectionPropertiesEditor = new ConnectionPropertiesControl(paramsGroup, SWT.BORDER);
        connectionPropertySource = connectionPropertiesEditor.makeProperties(driver, driver.getConnectionProperties());
        connectionPropertiesEditor.loadProperties(connectionPropertySource);


        CTabItem paramsTab = new CTabItem(group, SWT.NONE);
        paramsTab.setText(UIConnectionMessages.dialog_edit_driver_tab_name_connection_properties);
        paramsTab.setToolTipText(UIConnectionMessages.dialog_edit_driver_tab_tooltip_connection_properties);
        paramsTab.setControl(paramsGroup);
    }

    private void createClientHomesTab(CTabFolder group) {
        clientHomesPanel = new ClientHomesPanel(group, SWT.NONE);
        clientHomesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

        CTabItem paramsTab = new CTabItem(group, SWT.NONE);
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

    private void createLicenseTab(CTabFolder group, String license) {
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

        CTabItem paramsTab = new CTabItem(group, SWT.NONE);
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
        libTable.setInput(libraries);
        boolean hasFiles = false, hasDownloads = false;
        for (DBPDriverLibrary library : libraries) {
            final Path localFile = library.getLocalFile();
            hasFiles = hasFiles || (localFile != null && Files.exists(localFile));
            if (!hasFiles) {
                final Collection<DriverDescriptor.DriverFileInfo> files = driver.getLibraryFiles(library);
                if (files != null) {
                    for (DriverDescriptor.DriverFileInfo file : files) {
                        if (file.getFile() != null && Files.exists(file.getFile())) {
                            hasFiles = true;
                        }
                    }
                }
            }

            if (library.isDownloadable()) {
                hasDownloads = true;
            }
        }
        findClassButton.setEnabled(provider.isDriversManagable() && hasFiles);
        if (updateVersionButton != null) {
            updateVersionButton.setEnabled(hasDownloads);
        }
        detailsButton.setEnabled(hasFiles);
        classListCombo.setEnabled(hasFiles);
    }

    private void changeLibSelection() {
        DriverLibraryAbstract selectedLib = getSelectedLibrary();
        detailsButton.setEnabled(selectedLib != null);
        deleteButton.setEnabled(selectedLib != null);
        if (editButton != null) {
            editButton.setEnabled(selectedLib instanceof DriverLibraryMavenArtifact);
        }

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
        driverNameText.setText(CommonUtils.notEmpty(original ? driver.getOrigName() : driver.getName()));
        driverDescText.setText(CommonUtils.notEmpty(original ? driver.getOrigDescription() : driver.getDescription()));
        driverClassText.setText(CommonUtils.notEmpty(original ? driver.getOrigClassName() : driver.getDriverClassName()));
        driverURLText.setText(CommonUtils.notEmpty(original ? driver.getOrigSampleURL() : driver.getSampleURL()));
        driverPortText.setText(CommonUtils.notEmpty(original ? driver.getOrigDefaultPort() : driver.getDefaultPort()));
        driverDatabaseText.setText(CommonUtils.notEmpty(original ? driver.getOrigDefaultDatabase() : driver.getDefaultDatabase()));
        driverUserText.setText(CommonUtils.notEmpty(original ? driver.getOrigDefaultUser() : driver.getDefaultUser()));
        propagateDriverPropertiesCheck.setSelection(original ? driver.isOrigPropagateDriverProperties() : driver.isPropagateDriverProperties());
        embeddedDriverCheck.setSelection(original ? driver.isOrigEmbedded() : driver.isEmbedded());
        anonymousDriverCheck.setSelection(original ? driver.isOrigAnonymousAccess() : driver.isAnonymousAccess());
        allowsEmptyPasswordCheck.setSelection(original ? driver.isOrigAllowsEmptyPassword() : driver.isAllowsEmptyPassword());
        nonInstantiableCheck.setSelection(original ? !driver.isOrigInstantiable() : !driver.isInstantiable());
        threadSafeCheck.setSelection(driver.isThreadSafeDriver());

        if (original) {
            resetLibraries();
        }
        if (libTable != null) {
            libTable.setInput(libraries);
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
        //resetLibraries(false);
        super.cancelPressed();
    }

    private void resetLibraries() {
        libraries.clear();
        libraries.addAll(driver.getOrigFiles());
    }

    @Override
    protected void okPressed() {
        saveDriverSettings(this.driver);

        DriverDescriptor oldDriver = provider.getDriverByName(driver.getCategory(), driver.getName());
        if (oldDriver != null && oldDriver != driver && !oldDriver.isDisabled() && oldDriver.getReplacedBy() == null) {
            UIUtils.showMessageBox(getShell(), UIConnectionMessages.dialog_edit_driver_dialog_save_exists_title, NLS.bind(UIConnectionMessages.dialog_edit_driver_dialog_save_exists_message, driver.getName()), SWT.ICON_ERROR);
            return;
        }

        if (DBWorkbench.isDistributed()) {
            try {
                syncDriverLibraries();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Error saving driver", "Driver libraries sync failed", e);
                return;
            }
        }

        // Finish
        if (provider.getDriver(driver.getId()) == null) {
            provider.addDriver(driver);
        }
        provider.getRegistry().saveDrivers();

        if (DBWorkbench.isDistributed()) {
            try {
                UIUtils.runInProgressDialog(monitor -> {
                    try {
                        driver.getDriverInstance(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Error resolving driver files", "Driver cannot be instantiated", e);
            }
        }

        super.okPressed();
    }

    private void saveDriverSettings(DriverDescriptor drv) {
        // Set props
        drv.setName(driverNameText.getText());
        drv.setDescription(CommonUtils.notEmpty(driverDescText.getText()));
        drv.setDriverClassName(driverClassText.getText());
        drv.setSampleURL(driverURLText.getText());
        drv.setDriverDefaultPort(driverPortText.getText());
        drv.setDriverDefaultDatabase(driverDatabaseText.getText());
        drv.setDriverDefaultUser(driverUserText.getText());
        drv.setEmbedded(embeddedDriverCheck.getSelection());
        drv.setPropagateDriverProperties(propagateDriverPropertiesCheck.getSelection());
        drv.setAnonymousAccess(anonymousDriverCheck.getSelection());
        drv.setAllowsEmptyPassword(allowsEmptyPasswordCheck.getSelection());
        drv.setInstantiable(!nonInstantiableCheck.getSelection());
        drv.setThreadSafeDriver(threadSafeCheck.getSelection());

//        driver.setAnonymousAccess(anonymousCheck.getSelection());
        drv.setModified(true);

        drv.setDriverParameters(driverPropertySource.getPropertiesWithDefaults());
        drv.setConnectionProperties(connectionPropertySource.getPropertyValues());

        // Store client homes
        if (clientHomesPanel != null) {
            drv.setNativeClientLocations(clientHomesPanel.getLocalLocations());
        }

        drv.setDriverLibraries(libraries);
    }

    // Distributed products only!
    private void syncDriverLibraries() throws DBException {
        List<DBPDriverLibrary> oldLibs = driver.getEnabledDriverLibraries();
        for (DBPDriverLibrary oldLib : oldLibs) {
            if (!libraries.contains(oldLib)) {
                // Remove old library files
                List<DBPDriver> usedBy = DriverEditHelpers.getDriversByLibrary(oldLib);
                if (usedBy.size() <= 1) {
                    syncRemoveDriverLibFile(oldLib);
                }
            }
        }
        for (DBPDriverLibrary newLib : libraries) {
            if (!(newLib instanceof DriverLibraryLocal)) {
                log.error("Wrong driver library found: " + newLib + ". Must be a local file");
                continue;
            }
            // Add new library files
            Path localFilePath = Path.of(newLib.getPath());
            String shortFileName = localFilePath.getFileName().toString();
            if (!Files.exists(localFilePath)) {
                log.error("Driver library doesn't exist: " + localFilePath + ".");
                continue;
            }
            if (Files.isDirectory(localFilePath)) {
                synAddDriverLibDirectory(newLib, localFilePath, shortFileName);
            } else {
                syncAddDriverLibFile(newLib, localFilePath, shortFileName);
            }
        }
    }

    private void syncRemoveDriverLibFile(DBPDriverLibrary library) throws DBException {
        Collection<DriverDescriptor.DriverFileInfo> libraryFiles = driver.getLibraryFiles(library);
        if (libraryFiles == null) {
            return;
        }
        DBFileController fileController = DBWorkbench.getPlatform().getFileController();

        for (DriverDescriptor.DriverFileInfo file : libraryFiles) {
            fileController.deleteFile(
                DBFileController.TYPE_DATABASE_DRIVER,
                file.getFile().toString(),
                false);
        }
    }

    private void synAddDriverLibDirectory(DBPDriverLibrary newLib, Path localFilePath, String shortFileName) throws DBException {
        try (Stream<Path> list = Files.list(localFilePath)) {
            for (Path file : list.toList()) {
                shortFileName = shortFileName + "/" + file.getFileName().toString();
                if (Files.isDirectory(file)) {
                    synAddDriverLibDirectory(newLib, file, shortFileName + "/" + file.getFileName().toString());
                } else {
                    syncAddDriverLibFile(newLib, file, shortFileName);
                }
            }
        } catch (IOException e) {
            throw new DBException("Error sync lib directory", e);
        }
    }

    private void syncAddDriverLibFile(DBPDriverLibrary library, Path localFilePath, String shortFileName) throws DBException {
        DBFileController fileController = DBWorkbench.getPlatform().getFileController();

        String driverFilePath = driver.getId() + "/" + shortFileName;
        if (library instanceof DriverLibraryLocal libraryLocal) {
            libraryLocal.setPath(driverFilePath);
        }

        try {
            byte[] fileData = Files.readAllBytes(localFilePath);
            fileController.saveFileData(
                DBFileController.TYPE_DATABASE_DRIVER,
                driverFilePath,
                fileData);
        } catch (IOException e) {
            throw new DBException("IO error while saving driver file", e);
        }
        DriverDescriptor.DriverFileInfo fileInfo = new DriverDescriptor.DriverFileInfo(
            driverFilePath, null, DBPDriverLibrary.FileType.jar, Path.of(driverFilePath));
        fileInfo.setFileCRC(DriverDescriptor.calculateFileCRC(localFilePath));
        driver.addLibraryFile(library, fileInfo);
    }


    private class LibContentProvider implements ITreeContentProvider {
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
                return files.toArray(new Object[0]);
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
