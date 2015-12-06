/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryAbstract;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesPanel;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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
    static final Log log = Log.getLog(DriverEditDialog.class);

    private static final String DIALOG_ID = "DBeaver.DriverEditDialog";//$NON-NLS-1$

    private final boolean newDriver;
    private DataSourceProviderDescriptor provider;
    private DriverDescriptor driver;

    private String defaultCategory;
    private String curFolder = null;
    private TreeViewer libTable;
    private Button deleteButton;
    private Button updateVersionButton;
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

    public DriverEditDialog(Shell shell, DriverDescriptor driver)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.driver = driver;
        this.provider = driver.getProviderDescriptor();
        this.defaultCategory = driver.getCategory();
        this.newDriver = false;
        this.origLibList = new ArrayList<>(driver.getDriverLibraries());
    }

    public DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider, String category)
    {
        super(shell, IHelpContextIds.CTX_DRIVER_EDITOR);
        this.provider = provider;
        this.driver = provider.createDriver();
        this.defaultCategory = category;
        this.newDriver = true;
        this.origLibList = new ArrayList<>();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
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
        if (newDriver) {
            getShell().setText(CoreMessages.dialog_edit_driver_title_create_driver);
        } else {
            getShell().setText(CoreMessages.dialog_edit_driver_title_edit_driver + driver.getName() + "'"); //$NON-NLS-2$
            getShell().setImage(DBeaverIcons.getImage(driver.getPlainIcon()));
        }

        boolean isReadOnly = !provider.isDriversManagable();
        int advStyle = isReadOnly ? SWT.READ_ONLY : SWT.NONE;

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        group.setLayoutData(gd);

        {
            Group propsGroup = UIUtils.createControlGroup(group, "Settings", 4, -1, -1);
            propsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            gd = new GridData(GridData.FILL_HORIZONTAL);
            driverNameText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_driver_name + "*", driver.getName(), SWT.BORDER | advStyle, gd);
            driverNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            UIUtils.createControlLabel(propsGroup, "Driver Type");
            final CImageCombo providerCombo = new CImageCombo(propsGroup, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
            providerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (newDriver) {
                for (DataSourceProviderDescriptor p : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
                    if (p.isDriversManagable()) {
                        providerCombo.add(DBeaverIcons.getImage(p.getIcon()), p.getName(), null, p);
                    }
                }
                providerCombo.select(provider);
                providerCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        provider = (DataSourceProviderDescriptor) providerCombo.getItem(providerCombo.getSelectionIndex()).getData();
                        driver = provider.createDriver();
                    }
                });
            } else {
                providerCombo.add(DBeaverIcons.getImage(provider.getIcon()), provider.getName(), null, provider);
                providerCombo.select(provider);
            }

            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            driverClassText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_class_name + "*", CommonUtils.notEmpty(driver.getDriverClassName()), SWT.BORDER | advStyle, gd);
            driverClassText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverURLText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_sample_url, CommonUtils.notEmpty(driver.getSampleURL()), SWT.BORDER | advStyle, gd);
            driverURLText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            gd = new GridData(GridData.FILL_HORIZONTAL);
            driverPortText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_default_port, driver.getDefaultPort() == null ? "" : driver.getDefaultPort(), SWT.BORDER | advStyle, gd);
            driverPortText.setLayoutData(new GridData(SWT.NONE));
            driverPortText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            gd = new GridData(GridData.FILL_HORIZONTAL);
            embeddedDriverCheck = UIUtils.createLabelCheckbox(propsGroup, "Embedded", "Embedded driver", driver.isEmbedded());
            embeddedDriverCheck.setLayoutData(gd);
        }

        {
            Group infoGroup = UIUtils.createControlGroup(group, "Description", 2, -1, -1);
            infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            driverCategoryCombo = UIUtils.createLabelCombo(infoGroup, CoreMessages.dialog_edit_driver_label_category, SWT.BORDER | SWT.DROP_DOWN | advStyle);
            driverCategoryCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            if (isReadOnly) {
                driverCategoryCombo.setEnabled(false);
            }
            Set<String> categories = new TreeSet<>();
            for (DataSourceProviderDescriptor p : DataSourceProviderRegistry.getInstance().getDataSourceProviders()) {
                for (DriverDescriptor drv : p.getEnabledDrivers()) {
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

            driverDescText = UIUtils.createLabelText(infoGroup, CoreMessages.dialog_edit_driver_label_description, CommonUtils.notEmpty(driver.getDescription()), SWT.BORDER | advStyle);

            if (!CommonUtils.isEmpty(driver.getWebURL())) {
                UIUtils.createControlLabel(infoGroup, CoreMessages.dialog_edit_driver_label_website);
                Link urlLabel = UIUtils.createLink(infoGroup, "<a>" + driver.getWebURL() + "</a>", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        RuntimeUtils.launchProgram(driver.getWebURL());
                    }
                });
                urlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
        }

        {
            TabFolder tabFolder = new TabFolder(group, SWT.NONE);
            tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (provider.isDriversManagable()) {
                createLibrariesTab(tabFolder);
            }
            createConnectionPropertiesTab(tabFolder);
            createParametersTab(tabFolder);
            // Client homes
            if (driver.getClientManager() != null) {
                createClientHomesTab(tabFolder);
            }

            final String license = driver.getLicense();
            if (license != null) {
                createLicenseTab(tabFolder, license);
            }

            tabFolder.setSelection(0);
        }

        loadSettings(false);

        return group;
    }

    private void createLibrariesTab(TabFolder group)
    {
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
            libTable = new TreeViewer(libsListGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            libTable.setContentProvider(new LibContentProvider());
            libTable.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    final Object element = cell.getElement();
                    if (element instanceof DBPDriverLibrary) {
                        DBPDriverLibrary lib = (DBPDriverLibrary) element;
                        cell.setText(lib.getDisplayName());
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
                        cell.setImage(DBeaverIcons.getImage(UIIcon.JAR));
                    }
                }
            });
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event)
                {
                    changeLibSelection();
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

        UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_add_file, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                fd.setText(CoreMessages.dialog_edit_driver_dialog_open_driver_library);
                fd.setFilterPath(curFolder);
                String[] filterExt = {"*.jar;*.zip", "*.dll;*.so", "*.*"}; //$NON-NLS-1$ //$NON-NLS-2$
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
                                    new File(folderFile, fileName).getAbsolutePath()));
                        }
                        changeLibContent();
                    }
                }
            }
        });

        UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_add_folder, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.MULTI);
                fd.setText(CoreMessages.dialog_edit_driver_dialog_open_driver_directory);
                fd.setFilterPath(curFolder);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    driver.addDriverLibrary(DriverLibraryAbstract.createFromPath(
                        driver,
                        DBPDriverLibrary.FileType.jar,
                        selected));
                    changeLibContent();
                }
            }
        });

        UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_add_artifact, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                EditMavenArtifactDialog fd = new EditMavenArtifactDialog(getShell(), null);
                if (fd.open() == IDialogConstants.OK_ID) {
                    driver.addDriverLibrary(DriverLibraryAbstract.createFromPath(
                        driver,
                        DBPDriverLibrary.FileType.jar,
                        DriverLibraryMavenArtifact.PATH_PREFIX + fd.getArtifact().getPath()));
                    changeLibContent();
                }
            }
        });

        updateVersionButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_update_version, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                driver.updateFiles();
                changeLibContent();
            }
        });

        deleteButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_delete, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                driver.removeDriverLibrary(getSelectedLibrary());
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

/*
        upButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_up, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DriverLibraryAbstract selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex - 1);
                changeLibContent();
                changeLibSelection();
            }
        });
        upButton.setEnabled(false);

        downButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_down, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DriverLibraryAbstract selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex + 1);
                changeLibContent();
                changeLibSelection();
            }
        });
        downButton.setEnabled(false);
*/

        UIUtils.createHorizontalLine(libsControlGroup);

        UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_classpath, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
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

    private DriverLibraryAbstract getSelectedLibrary()
    {
        IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
        if (selection != null && !selection.isEmpty()) {
            final Object element = selection.getFirstElement();
            if (element instanceof DriverLibraryAbstract) {
                return (DriverLibraryAbstract) element;
            }
        }
        return null;
    }

    private void changeLibContent()
    {
        libTable.setInput(driver.getEnabledDriverLibraries());
        boolean hasFiles = false, hasDownloads = false;
        for (DBPDriverLibrary library : driver.getDriverLibraries()) {
            if (library.isDownloadable()) {
                hasDownloads = true;
                break;
            }
            hasFiles = true;
        }
        findClassButton.setEnabled(provider.isDriversManagable() && hasFiles);
        updateVersionButton.setEnabled(hasDownloads);
    }

    private void changeLibSelection()
    {
        DriverLibraryAbstract selectedLib = getSelectedLibrary();
        deleteButton.setEnabled(selectedLib != null);
/*
        upButton.setEnabled(libList.indexOf(selectedLib) > 0);
        downButton.setEnabled(libList.indexOf(selectedLib) < libList.size() - 1);
*/
    }

    private void onChangeProperty()
    {
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(
                !CommonUtils.isEmpty(driverNameText.getText()) &&
                    !CommonUtils.isEmpty(driverClassText.getText()));
        }
    }

    private void loadSettings(boolean original)
    {
        driverNameText.setText(original ? CommonUtils.notEmpty(driver.getOrigName()) : CommonUtils.notEmpty(driver.getName()));
        driverDescText.setText(original ? CommonUtils.notEmpty(driver.getOrigDescription()) : CommonUtils.notEmpty(driver.getDescription()));
        driverClassText.setText(original ? CommonUtils.notEmpty(driver.getOrigClassName()) : CommonUtils.notEmpty(driver.getDriverClassName()));
        driverURLText.setText(original ? CommonUtils.notEmpty(driver.getOrigSampleURL()) : CommonUtils.notEmpty(driver.getSampleURL()));
        driverPortText.setText(original ?
            (driver.getOrigDefaultPort() == null ? "" : driver.getOrigDefaultPort()) : //$NON-NLS-1$
            (driver.getDefaultPort() == null ? "" : driver.getDefaultPort())); //$NON-NLS-1$

        embeddedDriverCheck.setSelection(driver.isEmbedded());

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
        if (clientHomesPanel != null) {
            clientHomesPanel.loadHomes(driver);
        }
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
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
            driver.addDriverLibrary(lib);
        }
        for (DBPDriverLibrary lib : CommonUtils.copyList(driver.getDriverLibraries())) {
            if (!newLibList.contains(lib)) {
                driver.removeDriverLibrary(lib);
            }
        }
    }

    @Override
    protected void okPressed()
    {
        // Set props
        driver.setName(driverNameText.getText());
        driver.setCategory(driverCategoryCombo.getText());
        driver.setDescription(CommonUtils.notEmpty(driverDescText.getText()));
        driver.setDriverClassName(driverClassText.getText());
        driver.setSampleURL(driverURLText.getText());
        driver.setDriverDefaultPort(driverPortText.getText());
        driver.setEmbedded(embeddedDriverCheck.getSelection());
//        driver.setAnonymousAccess(anonymousCheck.getSelection());
        driver.setModified(true);

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

        super.okPressed();
    }

    public static void showBadConfigDialog(final Shell shell, final String message, final DBException error) {
        //log.debug(message);
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                DBPDataSource dataSource = error.getDataSource();
                String title = "Bad driver [" + dataSource.getContainer().getDriver().getName() + "] configuration";
                new BadDriverConfigDialog(shell, title, message == null ? title : message, error).open();
            }
        };
        UIUtils.runInUI(shell, runnable);
    }

    private class ClassFindJob implements IRunnableWithProgress {

        public static final String SQL_DRIVER_CLASS_NAME = "java/sql/Driver";
        public static final String OBJECT_CLASS_NAME = "java/lang/Object";
        public static final String CLASS_FILE_EXT = ".class";
        private java.util.List<String> driverClassNames = new ArrayList<>();

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
            java.util.List<File> libFiles = new ArrayList<>();
            java.util.List<URL> libURLs = new ArrayList<>();
            for (DBPDriverLibrary lib : driver.getDriverLibraries()) {
                File libFile = lib.getLocalFile();
                if (libFile != null && libFile.exists() && !libFile.isDirectory() && lib.getType() == DBPDriverLibrary.FileType.jar) {
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
                        if (fileName.endsWith(CLASS_FILE_EXT) && !fileName.contains("$")) { //$NON-NLS-1$ //$NON-NLS-2$
                            String className = fileName.replaceAll("/", ".").replace(CLASS_FILE_EXT, ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            monitor.subTask(className);
                            try {
                                if (implementsDriver(currentFile, current, 0)) {
                                    driverClassNames.add(className);
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

        private boolean implementsDriver(JarFile currentFile, JarEntry current, int depth) throws IOException {
            try (InputStream classStream = currentFile.getInputStream(current)) {
                ClassReader cr = new ClassReader(classStream);
                int access = cr.getAccess();
                if (depth == 0 && ((access & Opcodes.ACC_PUBLIC) == 0 || (access & Opcodes.ACC_ABSTRACT) != 0)) {
                    return false;
                }
                String[] interfaces = cr.getInterfaces();
                if (ArrayUtils.contains(interfaces, SQL_DRIVER_CLASS_NAME)) {
                    return true;
                } else if (!CommonUtils.isEmpty(cr.getSuperName()) && !cr.getSuperName().equals(OBJECT_CLASS_NAME)) {
                    // Check recursively
                    JarEntry jarEntry = currentFile.getJarEntry(cr.getSuperName() + CLASS_FILE_EXT);
                    if (jarEntry != null) {
                        return implementsDriver(currentFile, jarEntry, depth + 1);
                    }
                }
                for (String intName : interfaces) {
                    JarEntry jarEntry = currentFile.getJarEntry(intName + CLASS_FILE_EXT);
                    if (jarEntry != null) {
                        if (implementsDriver(currentFile, jarEntry, depth + 1)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

    }

    private static class BadDriverConfigDialog extends StandardErrorDialog {

        private final DBPDataSource dataSource;

        public BadDriverConfigDialog(Shell shell, String title, String message, DBException error)
        {
            super(
                shell == null ? DBeaverUI.getActiveWorkbenchShell() : shell,
                title,
                message,
                RuntimeUtils.stripStack(GeneralUtils.makeExceptionStatus(error)),
                IStatus.ERROR);
            dataSource = error.getDataSource();
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.RETRY_ID, "Open Driver Configuration", true);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
            createDetailsButton(parent);
        }

        @Override
        protected void buttonPressed(int id)
        {
            if (id == IDialogConstants.RETRY_ID) {
                UIUtils.runInDetachedUI(getShell(), new Runnable() {
                    @Override
                    public void run() {
                        DriverEditDialog dialog = new DriverEditDialog(
                            DBeaverUI.getActiveWorkbenchShell(),
                            (DriverDescriptor) dataSource.getContainer().getDriver());
                        dialog.open();
                    }
                });
                super.buttonPressed(IDialogConstants.OK_ID);
            }
            super.buttonPressed(id);
        }
    }

    private class LibContentProvider implements ITreeContentProvider {
        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
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
            if (element instanceof DBPDriverLibrary) {
                return !CommonUtils.isEmpty(driver.getLibraryFiles((DBPDriverLibrary) element));
            }
            return false;
        }
    }
}
