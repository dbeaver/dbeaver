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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverFileSource;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

class DriverDownloadManualPage extends DriverDownloadPage {

    private DriverFileSource fileSource;
    private Table filesTable;

    DriverDownloadManualPage() {
        super("Configure driver files", "Download driver files", null);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        final DriverDescriptor driver = getWizard().getDriver();

        setMessage("Download & configure " + driver.getFullName() + " driver files");

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Link infoText = new Link(composite, SWT.NONE);
        infoText.setText(driver.getFullName() + " driver files missing.\n\n" +
            "According to vendor policy this driver isn't publicly available\nand you have to download it manually from vendor's web site.\n\n" +
            "After successful driver download you will need to <a>add JAR files</a> in DBeaver libraries list.");
        infoText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getWizard().getContainer().buttonPressed(DriverDownloadDialog.EDIT_DRIVER_BUTTON_ID);
            }
        });
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        infoText.setLayoutData(gd);

        Group filesGroup = UIUtils.createControlGroup(composite, "Driver files", 1, -1, -1);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 10;
        filesGroup.setLayoutData(gd);

        final Combo sourceCombo = new Combo(filesGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (DriverFileSource source : driver.getDriverFileSources()) {
            sourceCombo.add(source.getName());
        }
        final Link driverLink = new Link(filesGroup, SWT.NONE);
        driverLink.setText("<a>" + driver.getDriverFileSources().get(0).getUrl() + "</a>");
        driverLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        driverLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                WebUtils.openWebBrowser(driver.getDriverFileSources().get(sourceCombo.getSelectionIndex()).getUrl());
            }
        });

        filesTable = new Table(filesGroup, SWT.BORDER | SWT.FULL_SELECTION);
        filesTable.setHeaderVisible(true);
        filesTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createTableColumn(filesTable, SWT.LEFT, "File");
        UIUtils.createTableColumn(filesTable, SWT.LEFT, "Required");
        UIUtils.createTableColumn(filesTable, SWT.LEFT, "Description");

        sourceCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectFileSource(driver.getDriverFileSources().get(sourceCombo.getSelectionIndex()));
                driverLink.setText("<a>" + driver.getDriverFileSources().get(sourceCombo.getSelectionIndex()).getUrl() + "</a>");
            }
        });

        sourceCombo.select(0);
        selectFileSource(driver.getDriverFileSources().get(0));
        UIUtils.packColumns(filesTable, true);

        createLinksPanel(composite);

        composite.setTabList(ArrayUtils.remove(Control.class, composite.getTabList(), infoText));

        setControl(composite);
    }

    private void selectFileSource(DriverFileSource source) {
        fileSource = source;
        filesTable.removeAll();
        for (DriverFileSource.FileInfo file : fileSource.getFiles()) {
            new TableItem(filesTable, SWT.NONE).setText(new String[] {
                file.getName(),
                !file.isOptional() ? "Yes" : "No",
                CommonUtils.notEmpty(file.getDescription()) });
        }
    }

    @Override
    public boolean isPageComplete() {
        return fileSource != null;
    }

    @Override
    void resolveLibraries() {
        // do nothing
    }

    @Override
    boolean performFinish() {
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                WebUtils.openWebBrowser(fileSource.getUrl());
            }
        });
        return false;
//        DriverEditDialog dialog = new DriverEditDialog(null, getWizard().getDriver());
//        dialog.open();
    }

}