/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPClientHome;
import org.jkiss.dbeaver.model.DBPClientManager;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ClientHomesControl
 */
public class ClientHomesControl extends Composite
{
    static final Log log = LogFactory.getLog(ClientHomesControl.class);

    private Table homesTable;
    private Text idText;
    private Text pathText;
    private Text nameText;
    private Text productNameText;
    private Text productVersionText;
    private Button addButton;
    private Button removeButton;

    public ClientHomesControl(
        Composite parent,
        int style)
    {
        super(parent, style);

        GridLayout layout = new GridLayout(2, false);
        setLayout(layout);

        Composite listGroup = UIUtils.createPlaceholder(this, 1);
        listGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable = new Table(listGroup, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        homesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        homesTable.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] selection = homesTable.getSelection();
                if (CommonUtils.isEmpty(selection)) {
                    selectHome(null);
                } else {
                    selectHome((DBPClientHome)selection[0].getData());
                }
            }
        });
        Composite buttonsGroup = UIUtils.createPlaceholder(listGroup, 2);
        buttonsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        addButton = new Button(buttonsGroup, SWT.PUSH);
        addButton.setText("Add Home");
        removeButton = new Button(buttonsGroup, SWT.PUSH);
        removeButton.setText("Remove Home");
        removeButton.setEnabled(false);

        Group infoGroup = UIUtils.createControlGroup(this, "Information", 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        idText = UIUtils.createLabelText(infoGroup, "ID", "", SWT.BORDER | SWT.READ_ONLY);
        pathText = UIUtils.createLabelText(infoGroup, "Path", "", SWT.BORDER | SWT.READ_ONLY);
        nameText = UIUtils.createLabelText(infoGroup, "Name", "", SWT.BORDER | SWT.READ_ONLY);
        productNameText = UIUtils.createLabelText(infoGroup, "Product Name", "", SWT.BORDER | SWT.READ_ONLY);
        productVersionText = UIUtils.createLabelText(infoGroup, "Product Version", "", SWT.BORDER | SWT.READ_ONLY);
    }

    private void selectHome(DBPClientHome home)
    {
        removeButton.setEnabled(home != null);
        idText.setText(home == null ? "" : home.getHomeId());
        pathText.setText(home == null ? "" : home.getHomePath());
        nameText.setText(home == null ? "" : home.getDisplayName());
        try {
            productNameText.setText(home == null ? "" : home.getProductName());
        } catch (DBException e) {
            log.warn(e);
        }
        try {
            productVersionText.setText(home == null ? "" : home.getProductVersion());
        } catch (DBException e) {
            log.warn(e);
        }
    }

    public void loadHomes(DriverDescriptor driver)
    {
        DBPClientManager clientManager = driver.getClientManager();
        if (clientManager == null) {
            log.error("Client manager is not supported by driver '" + driver.getName() + "'");
            return;
        }
        Set<String> clientHomeIds = new LinkedHashSet<String>();
        clientHomeIds.addAll(clientManager.findClientHomeIds());
        clientHomeIds.addAll(driver.getClientHomeIds());

        for (String homeId : clientHomeIds) {
            DBPClientHome home;
            try {
                home = clientManager.getClientHome(homeId);
                if (home == null) {
                    log.warn("Home '" + homeId + "' is not supported");
                    continue;
                }
            } catch (Exception e) {
                log.error(e);
                continue;
            }
            TableItem homeItem = new TableItem(homesTable, SWT.NONE);
            homeItem.setText(home.getDisplayName());
            homeItem.setImage(DBIcon.HOME.getImage());
            homeItem.setData(home);
        }
    }

}