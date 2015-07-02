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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriverFileType;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ClientHomesPanel;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
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
    private Button embeddedDriverCheck;
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
        if (driver == null) {
            getShell().setText(CoreMessages.dialog_edit_driver_title_create_driver);
            driver = provider.createDriver();
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
            Composite propsGroup = new Composite(group, SWT.NONE);
            propsGroup.setLayout(new GridLayout(2, false));
            propsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(propsGroup, CoreMessages.dialog_edit_driver_label_driver_name);
            final Composite namePlaceholder = UIUtils.createPlaceholder(propsGroup, 3, 5);
            namePlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));

            driverNameText = new Text(namePlaceholder, SWT.BORDER | advStyle);
            driverNameText.setText(CommonUtils.notEmpty(driver.getName()));
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

            driverDescText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_description, CommonUtils.notEmpty(driver.getDescription()), SWT.BORDER | advStyle);

            driverClassText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_class_name, CommonUtils.notEmpty(driver.getDriverClassName()), SWT.BORDER | advStyle);
            driverClassText.addModifyListener(new ModifyListener()
            {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverURLText = UIUtils.createLabelText(propsGroup, CoreMessages.dialog_edit_driver_label_sample_url, CommonUtils.notEmpty(driver.getSampleURL()), SWT.BORDER | advStyle);
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
            ph.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
                urlLabel.setText("<a>" + driver.getWebURL() + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
                urlLabel.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        RuntimeUtils.launchProgram(driver.getWebURL());
                    }
                });
                urlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            }

            embeddedDriverCheck = UIUtils.createLabelCheckbox(propsGroup, "Embedded", "Embedded driver", driver.isEmbedded());
            embeddedDriverCheck.setEnabled(!isReadOnly);
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
                                DBeaverIcons.getImage(DBIcon.TREE_FOLDER) :
                                DBeaverIcons.getImage((lib.getType() == DBPDriverFileType.jar ? UIIcon.JAR : DBIcon.TYPE_UNKNOWN)));
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

        UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_add_file, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
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
                    if (!ArrayUtils.isEmpty(fileNames)) {
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
                    libList.add(new DriverFileDescriptor(
                        driver,
                        DBPDriverFileType.jar,
                        selected));
                    changeLibContent();
                }
            }
        });

        deleteButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_delete, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                libList.remove(getSelectedLibrary());
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

        upButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_up, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DriverFileDescriptor selectedLib = getSelectedLibrary();
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
                DriverFileDescriptor selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex + 1);
                changeLibContent();
                changeLibSelection();
            }
        });
        downButton.setEnabled(false);

        Button cpButton = UIUtils.createToolButton(libsControlGroup, CoreMessages.dialog_edit_driver_button_classpath, new SelectionAdapter() {
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
        driverNameText.setText(CommonUtils.notEmpty(driver.getOrigName()));
        driverDescText.setText(CommonUtils.notEmpty(driver.getOrigDescription()));
        driverClassText.setText(CommonUtils.notEmpty(driver.getOrigClassName()));
        driverURLText.setText(CommonUtils.notEmpty(driver.getOrigSampleURL()));
        driverPortText.setText(driver.getOrigDefaultPort() == null ? "" : driver.getOrigDefaultPort()); //$NON-NLS-1$
        if (!CommonUtils.isEmpty(driver.getCategory())) {
            driverCategoryCombo.setText(driver.getCategory());
        } else if (!CommonUtils.isEmpty(defaultCategory)) {
            driverCategoryCombo.setText(defaultCategory);
        }
        embeddedDriverCheck.setSelection(driver.isEmbedded());
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
        if (clientHomesPanel != null) {
            clientHomesPanel.loadHomes(driver);
        }
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
        driver.setDescription(CommonUtils.notEmpty(driverDescText.getText()));
        driver.setDriverClassName(driverClassText.getText());
        driver.setSampleURL(driverURLText.getText());
        driver.setDriverDefaultPort(driverPortText.getText());
        driver.setEmbedded(embeddedDriverCheck.getSelection());
//        driver.setAnonymousAccess(anonymousCheck.getSelection());
        driver.setModified(true);

        // Set libraries
        for (DriverFileDescriptor lib : CommonUtils.safeCollection(libList)) {
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
            driver.loadDriver(DBeaverUI.getDefaultRunnableContext(), true);
        } catch (Throwable ex) {
            UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_edit_driver_dialog_driver_error_title, CoreMessages.dialog_edit_driver_dialog_driver_error_message, ex);
        }

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
            InputStream classStream = currentFile.getInputStream(current);
            try {
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
            } finally {
                classStream.close();
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
                DriverEditDialog dialog = new DriverEditDialog(getShell(), (DriverDescriptor) dataSource.getContainer().getDriver());
                dialog.open();
                super.buttonPressed(IDialogConstants.OK_ID);
            }
            super.buttonPressed(id);
        }
    }
}
