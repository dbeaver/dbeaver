/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.driver;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverLibraryDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.File;

/**
 * EditDriverDialog
 */
public class EditDriverDialog extends Dialog
{
    private DataSourceProviderDescriptor provider;
    private DriverDescriptor driver;
    private String curFolder = null;
    private ListViewer libList;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;
    private Text driverNameText;
    private Text driverDescText;
    private Text driverClassText;
    private Text driverURLText;
    private Text driverPortText;

    public EditDriverDialog(Shell shell, DriverDescriptor driver)
    {
        super(shell);
        this.driver = driver;
        this.provider = driver.getProviderDescriptor();
    }

    public EditDriverDialog(Shell shell, DataSourceProviderDescriptor provider)
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
            //driver.setSampleURL("jdbc:");
        } else {
            getShell().setText("Edit Driver '" + driver.getName() + "'");
        }

        boolean isReadOnly = !provider.isDriversManagable();
        int advStyle = isReadOnly ? SWT.READ_ONLY : SWT.NONE;

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 400;
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

            driverPortText = UIUtils.createLabelText(propsGroup, "Default Port", driver.getDefaultPort() == null ? "" : driver.getDefaultPort().toString(), SWT.BORDER | advStyle);
            driverPortText.setLayoutData(new GridData(SWT.NONE));
            driverPortText.addModifyListener(new ModifyListener()
            {
                public void modifyText(ModifyEvent e)
                {
                    onChangeProperty();
                }
            });
        }
        {
            Composite libsGroup = new Composite(group, SWT.BORDER);
            libsGroup.setLayout(new GridLayout(2, false));
            gd = new GridData(GridData.FILL_BOTH);
            libsGroup.setLayoutData(gd);

            Label libsLabel = UIUtils.createControlLabel(libsGroup, "Additional Driver Libraries");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            libsLabel.setLayoutData(gd);

            //ListViewer list = new ListViewer(libsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            libList = new ListViewer(libsGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            //libsTable.setLinesVisible (true);
            //libsTable.setHeaderVisible (true);
            libList.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            libList.getControl().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event event)
                {
                    changeLibSelection();
                }
            });

            for (DriverLibraryDescriptor lib : driver.getLibraries()) {
                if (lib.isDisabled()) {
                    continue;
                }
                libList.getList().add(lib.getLibraryFile().getPath());
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
                                libList.getList().add(new File(folderFile, fileName).getPath());
/*
                                TableItem item = new TableItem (libsTable, SWT.NONE);
                                    item.setText(
                                        new File(folderFile, fileName).getPath());
*/
                            }
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
                        libList.getList().add(selected);
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
                    libList.getList().remove(libList.getList().getSelectionIndices());
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
                    int selIndex = libList.getList().getSelectionIndex();
                    String selItem = libList.getList().getItem(selIndex);
                    String prevItem = libList.getList().getItem(selIndex - 1);
                    libList.getList().setItem(selIndex, prevItem);
                    libList.getList().setItem(selIndex - 1, selItem);
                    libList.getList().setSelection(selIndex - 1);
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
                    int selIndex = libList.getList().getSelectionIndex();
                    String selItem = libList.getList().getItem(selIndex);
                    String nextItem = libList.getList().getItem(selIndex + 1);
                    libList.getList().setItem(selIndex, nextItem);
                    libList.getList().setItem(selIndex + 1, selItem);
                    libList.getList().setSelection(selIndex + 1);
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
        }
        return group;
    }

    private void changeLibSelection()
    {
        List list = libList.getList();
        deleteButton.setEnabled(list.getSelectionCount() > 0);
        upButton.setEnabled(list.getSelectionCount() == 1 && list.getSelectionIndex() > 0);
        downButton.setEnabled(list.getSelectionCount() == 1 && list.getSelectionIndex() < list.getItemCount() - 1);
    }

    private void onChangeProperty()
    {
        getButton(IDialogConstants.OK_ID).setEnabled(
            !CommonUtils.isEmpty(driverNameText.getText()) &&
            !CommonUtils.isEmpty(driverClassText.getText()));
    }

    protected void okPressed()
    {
        // Check props
        Integer portNumber = null;
        if (!CommonUtils.isEmpty(driverPortText.getText())) {
            try {
                portNumber = new Integer(driverPortText.getText());
            }
            catch (NumberFormatException e) {
                DBeaverUtils.showErrorDialog(getShell(), "Invalid parameters", "Bad driver port specified");
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
        String[] libNames = libList.getList().getItems();
        File[] libFiles = new File[libNames.length];
        for (int i = 0; i < libNames.length; i++) {
            libFiles[i] = new File(libNames[i]);
        }
        for (File libFile : libFiles) {
            boolean exists = false;
            for (DriverLibraryDescriptor lib : driver.getLibraries()) {
                if (lib.getLibraryFile().equals(libFile)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                driver.addLibrary(libFile.getPath());
            }
        }
        for (DriverLibraryDescriptor lib : CommonUtils.copyList(driver.getLibraries())) {
            boolean exists = false;
            for (File libFile : libFiles) {
                if (lib.getLibraryFile().equals(libFile)) {
                    exists = true;
                }
            }
            if (!exists) {
                driver.removeLibrary(lib);
            }
        }

        if (provider.getDriver(driver.getId()) == null) {
            provider.addDriver(driver);
        }
        provider.getRegistry().saveDrivers();

        try {
            driver.loadDriver(true);
        } catch (DBException ex) {
            DBeaverUtils.showErrorDialog(getShell(), "Driver Error", "Can't load driver", ex);
        }

        super.okPressed();
    }

}
