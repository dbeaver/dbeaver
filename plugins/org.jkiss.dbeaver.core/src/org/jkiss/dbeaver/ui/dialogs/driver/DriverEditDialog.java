/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.driver;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverLibraryDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.proptree.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * DriverEditDialog
 */
public class DriverEditDialog extends Dialog
{
    static final Log log = LogFactory.getLog(DriverEditDialog.class);

    private DataSourceProviderDescriptor provider;
    private DriverDescriptor driver;
    private String curFolder = null;
    private TableViewer libTable;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;
    private Combo classListCombo;
    private Button findClassButton;
    private Text driverNameText;
    private Text driverDescText;
    private Text driverClassText;
    private Text driverURLText;
    private Text driverPortText;
    private EditablePropertiesControl parametersEditor;
    private ConnectionPropertiesControl connectionPropertiesEditor;
    private List<DriverLibraryDescriptor> libList;

    public DriverEditDialog(Shell shell, DriverDescriptor driver)
    {
        super(shell);
        this.driver = driver;
        this.provider = driver.getProviderDescriptor();
    }

    public DriverEditDialog(Shell shell, DataSourceProviderDescriptor provider)
    {
        super(shell);
        this.provider = provider;
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        onChangeProperty();
        return ctl;
    }

    protected Control createDialogArea(Composite parent)
    {
        if (driver == null) {
            getShell().setText("Create new driver");
            driver = provider.createDriver();
        } else {
            getShell().setText("Edit Driver '" + driver.getName() + "'");
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

            driverNameText = UIUtils.createLabelText(propsGroup, "Driver Name", CommonUtils.getString(driver.getName()), SWT.BORDER | advStyle);
            driverNameText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverDescText = UIUtils.createLabelText(propsGroup, "Description", CommonUtils.getString(driver.getDescription()), SWT.BORDER | advStyle);

            driverClassText = UIUtils.createLabelText(propsGroup, "Class Name", CommonUtils.getString(driver.getDriverClassName()), SWT.BORDER | advStyle);
            driverClassText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            driverURLText = UIUtils.createLabelText(propsGroup, "Sample URL", CommonUtils.getString(driver.getSampleURL()), SWT.BORDER | advStyle);
            driverURLText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });

            boolean hasSite = !CommonUtils.isEmpty(driver.getWebURL());

            UIUtils.createControlLabel(propsGroup, "Default Port");
            Composite ph = hasSite ? UIUtils.createPlaceholder(propsGroup, 3) : propsGroup;
            driverPortText = new Text(ph, SWT.BORDER | advStyle);
            driverPortText.setText(driver.getDefaultPort() == null ? "" : driver.getDefaultPort().toString());
            driverPortText.setLayoutData(new GridData(SWT.NONE));
            driverPortText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });
            if (hasSite) {
                GridLayout gl = (GridLayout)ph.getLayout();
                gl.horizontalSpacing = 5;

                UIUtils.createControlLabel(ph, "Website");
                Link urlLabel = new Link(ph, SWT.NONE);
                urlLabel.setText("<a>" + driver.getWebURL() + "</a>");
                urlLabel.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {
                        Program.launch(e.text);
                    }
                });

            }
        }
        if (!isReadOnly) {
            TabFolder tabFolder = new TabFolder(group, SWT.TOP);
            tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
            tabFolder.setLayout(new FillLayout());

            createLibrariesTab(tabFolder);
            createConnectionPropertiesTab(tabFolder);
            createParametersTab(tabFolder);

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
                    DriverLibraryDescriptor lib = (DriverLibraryDescriptor) cell.getElement();
                    cell.setText(lib.getPath());
                    if (lib.getLibraryFile().exists()) {
                        cell.setForeground(null);
                    } else {
                        cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    }
                    cell.setImage(
                        !lib.getLibraryFile().exists() ?
                            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE) :
                            lib.getLibraryFile().isDirectory() ?
                                PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER) :
                                DBIcon.JAR.getImage());
                }
            });
            libTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libTable.getControl().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event event)
                {
                    changeLibSelection();
                }
            });

            libList = new ArrayList<DriverLibraryDescriptor>();
            for (DriverLibraryDescriptor lib : driver.getLibraries()) {
                if (lib.isDisabled()) {
                    continue;
                }
                libList.add(lib);
            }
            libTable.setInput(libList);

            // Find driver class

            Composite findClassGroup = new Composite(libsListGroup, SWT.TOP);
            findClassGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            findClassGroup.setLayout(layout);

            UIUtils.createControlLabel(findClassGroup, "Driver class");
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
            findClassButton = new Button(findClassGroup, SWT.PUSH);
            findClassButton.setText("Find Class");
            findClassButton.addListener(SWT.Selection, new Listener()
            {
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
        }

        Composite libsControlGroup = new Composite(libsGroup, SWT.TOP);
        libsControlGroup.setLayout(new GridLayout(1, true));
        libsControlGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Button newButton = new Button(libsControlGroup, SWT.PUSH);
        newButton.setText("Add &File");
        newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        newButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                fd.setText("Open driver library");
                fd.setFilterPath(curFolder);
                String[] filterExt = {"*.jar", "*.*"};
                fd.setFilterExtensions(filterExt);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    String[] fileNames = fd.getFileNames();
                    if (!CommonUtils.isEmpty(fileNames)) {
                        File folderFile = new File(curFolder);
                        for (String fileName : fileNames) {
                            libList.add(
                                new DriverLibraryDescriptor(
                                    driver,
                                    new File(folderFile, fileName).getAbsolutePath()));
                        }
                        changeLibContent();
                    }
                }
            }
        });

        Button newDirButton = new Button(libsControlGroup, SWT.PUSH);
        newDirButton.setText("Add Fol&der");
        newDirButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        newDirButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.MULTI);
                fd.setText("Open driver directory");
                fd.setFilterPath(curFolder);
                String selected = fd.open();
                if (selected != null) {
                    curFolder = fd.getFilterPath();
                    libList.add(new DriverLibraryDescriptor(driver, selected));
                    changeLibContent();
                }
            }
        });

        deleteButton = new Button(libsControlGroup, SWT.PUSH);
        deleteButton.setText("D&elete");
        deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                libList.remove(getSelectedLibrary());
                changeLibContent();
            }
        });
        deleteButton.setEnabled(false);

        upButton = new Button(libsControlGroup, SWT.PUSH);
        upButton.setText("&Up");
        upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upButton.setEnabled(false);
        upButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                DriverLibraryDescriptor selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex - 1);
                changeLibContent();
                changeLibSelection();
            }
        });

        downButton = new Button(libsControlGroup, SWT.PUSH);
        downButton.setText("Do&wn");
        downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downButton.setEnabled(false);
        downButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                DriverLibraryDescriptor selectedLib = getSelectedLibrary();
                int selIndex = libList.indexOf(selectedLib);
                Collections.swap(libList, selIndex, selIndex + 1);
                changeLibContent();
                changeLibSelection();
            }
        });

        Button cpButton = new Button(libsControlGroup, SWT.PUSH);
        cpButton.setText("&Classpath");
        cpButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cpButton.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                ViewClasspathDialog cpDialog = new ViewClasspathDialog(getShell());
                cpDialog.open();
            }
        });

        changeLibContent();

        TabItem libsTab = new TabItem(group, SWT.NONE);
        libsTab.setText("Driver Libraries");
        libsTab.setToolTipText("Additional Driver Libraries");
        libsTab.setControl(libsGroup);
    }


    private void createParametersTab(TabFolder group)
    {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        parametersEditor = new EditablePropertiesControl(paramsGroup, SWT.NONE);
        parametersEditor.setMarginVisible(false);
        parametersEditor.loadProperties(driver.getProviderDescriptor().getDriverPropertyGroups(), driver.getDriverParameters(), driver.getDefaultDriverParameters());

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText("Advanced parameters");
        paramsTab.setToolTipText("Advanced driver parameters");
        paramsTab.setControl(paramsGroup);
    }

    private void createConnectionPropertiesTab(TabFolder group)
    {
        Composite paramsGroup = new Composite(group, SWT.NONE);
        paramsGroup.setLayout(new GridLayout(1, false));

        connectionPropertiesEditor = new ConnectionPropertiesControl(paramsGroup, SWT.NONE);
        connectionPropertiesEditor.setMarginVisible(false);
        connectionPropertiesEditor.loadProperties(driver, driver.getConnectionProperties());

        TabItem paramsTab = new TabItem(group, SWT.NONE);
        paramsTab.setText("Connection properties");
        paramsTab.setToolTipText("Default connection properties");
        paramsTab.setControl(paramsGroup);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (provider.isDriversManagable()) {
            Button resetButton = createButton(parent, IDialogConstants.RETRY_ID, "Reset to Defaults", false);
            if (driver.isCustom()) {
                resetButton.setEnabled(false);
            }
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
        }
    }

    private DriverLibraryDescriptor getSelectedLibrary()
    {
        IStructuredSelection selection = (IStructuredSelection) libTable.getSelection();
        return selection == null || selection.isEmpty() ? null : (DriverLibraryDescriptor) selection.getFirstElement();
    }

    private void changeLibContent()
    {
        libTable.refresh();
        findClassButton.setEnabled(!libList.isEmpty());
    }

    private void changeLibSelection()
    {
        DriverLibraryDescriptor selectedLib = getSelectedLibrary();
        deleteButton.setEnabled(selectedLib != null);
        upButton.setEnabled(libList.indexOf(selectedLib) > 0);
        downButton.setEnabled(libList.indexOf(selectedLib) < libList.size() - 1);
    }

    private void onChangeProperty()
    {
        if (provider.isDriversManagable()) {
            getButton(IDialogConstants.OK_ID).setEnabled(
                !CommonUtils.isEmpty(driverNameText.getText()) &&
                !CommonUtils.isEmpty(driverClassText.getText()));
        }
    }

    private void resetSettings()
    {
        driverNameText.setText(CommonUtils.getString(driver.getOrigName()));
        driverDescText.setText(CommonUtils.getString(driver.getOrigDescription()));
        driverClassText.setText(CommonUtils.getString(driver.getOrigClassName()));
        driverURLText.setText(CommonUtils.getString(driver.getOrigSampleURL()));
        driverPortText.setText(driver.getOrigDefaultPort() == null ? "" : driver.getOrigDefaultPort().toString());
        libList.clear();
        for (DriverLibraryDescriptor lib : driver.getOrigLibraries()) {
            if (lib.isDisabled()) {
                continue;
            }
            libList.add(lib);
        }
        changeLibContent();
        parametersEditor.loadProperties(driver.getProviderDescriptor().getDriverPropertyGroups(), driver.getDriverParameters(), driver.getDefaultDriverParameters());
        connectionPropertiesEditor.loadProperties(driver, driver.getConnectionProperties());
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

    protected void okPressed()
    {
        if (!provider.isDriversManagable()) {
            super.okPressed();
            return;
        }

        // Check props
        Integer portNumber = null;
        if (!CommonUtils.isEmpty(driverPortText.getText())) {
            try {
                portNumber = Integer.valueOf(driverPortText.getText());
            }
            catch (NumberFormatException e) {
                UIUtils.showErrorDialog(getShell(), "Invalid parameters", "Bad driver port specified");
                return;
            }
        }

        // Set props
        driver.setName(driverNameText.getText());
        driver.setDescription(CommonUtils.getString(driverDescText.getText()));
        driver.setDriverClassName(driverClassText.getText());
        driver.setSampleURL(driverURLText.getText());
        driver.setDriverDefaultPort(portNumber);
        driver.setModified(true);

        // Set libraries
        for (DriverLibraryDescriptor lib : libList) {
            driver.addLibrary(lib);
        }
        for (DriverLibraryDescriptor lib : CommonUtils.copyList(driver.getLibraries())) {
            if (!libList.contains(lib)) {
                driver.removeLibrary(lib);
            }
        }

        driver.setDriverParameters(parametersEditor.getProperties());
        driver.setConnectionProperties(connectionPropertiesEditor.getProperties());

        // Finish
        if (provider.getDriver(driver.getId()) == null) {
            provider.addDriver(driver);
        }
        provider.getRegistry().saveDrivers();

        try {
            driver.loadDriver(true);
        } catch (DBException ex) {
            UIUtils.showErrorDialog(getShell(), "Driver Error", "Can't load driver", ex);
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

        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            findDriverClasses(monitor);
        }

        private void findDriverClasses(IProgressMonitor monitor)
        {
            java.util.List<File> libFiles = new ArrayList<File>();
            java.util.List<URL> libURLs = new ArrayList<URL>();
            for (DriverLibraryDescriptor lib : libList) {
                File libFile = lib.getLibraryFile();
                if (libFile.exists() && !libFile.isDirectory()) {
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
                        if (fileName.endsWith(".class") && fileName.indexOf("$") == -1) {
                            String className = fileName.replaceAll("/", ".").replace(".class", "");
                            monitor.subTask(className);
                            try {
                                Class<?> aClass = Class.forName(className, false, findCL);
                                if (java.sql.Driver.class.isAssignableFrom(aClass)) {
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
