/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ui.preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPConnectionType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageConnectionTypes
 */
public class PrefPageConnectionTypes extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static final Log log = LogFactory.getLog(PrefPageConnectionTypes.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.connectionTypes"; //$NON-NLS-1$
    public static final String COLOR_TEXT = "               ";

    private Table typeTable;
    private Text typeName;
    private Text typeDescription;
    private CImageCombo colorPicker;
    private Button confirmSQL;
    private Button confirmDDL;
    private Button deleteButton;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(final Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            typeTable = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            typeTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createTableColumn(typeTable, SWT.LEFT, "Name");
            UIUtils.createTableColumn(typeTable, SWT.LEFT, "Description");
            typeTable.setHeaderVisible(true);
            typeTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            typeTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    selectType((DBPConnectionType) e.item.getData());
                }
            });

            Composite tableGroup = UIUtils.createPlaceholder(composite, 2, 5);
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Button newButton = new Button(tableGroup, SWT.PUSH);
            newButton.setText("New");

            deleteButton = new Button(tableGroup, SWT.PUSH);
            deleteButton.setText("Delete");
        }

        {
            Group groupSettings = UIUtils.createControlGroup(composite, "Settings", 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            groupSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            typeName = UIUtils.createLabelText(groupSettings, "Name", "");
            typeDescription = UIUtils.createLabelText(groupSettings, "Description", "");

            {
                UIUtils.createControlLabel(groupSettings, "Color");
                Composite colorGroup = UIUtils.createPlaceholder(groupSettings, 2, 5);
                colorGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                colorPicker = new CImageCombo(colorGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                colorPicker.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                Button pickerButton = new Button(colorGroup, SWT.PUSH);
                pickerButton.setText("...");
                pickerButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        DBPConnectionType connectionType = getSelectedType();
                        ColorDialog colorDialog = new ColorDialog(parent.getShell());
                        colorDialog.setRGB(connectionType.getColor().getRGB());
                        RGB rgb = colorDialog.open();
                        if (rgb != null) {

                        }
                    }
                });
            }

            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            confirmSQL = UIUtils.createCheckbox(groupSettings, "Confirm SQL execution", false);
            confirmSQL.setLayoutData(gd);
            confirmDDL = UIUtils.createCheckbox(groupSettings, "Confirm DDL execution", false);
            confirmDDL.setLayoutData(gd);
        }

        performDefaults();

        return composite;
    }

    private void selectType(DBPConnectionType connectionType)
    {
        colorPicker.select(connectionType.getColor());
        typeName.setText(connectionType.getName());
        typeDescription.setText(connectionType.getDescription());

        deleteButton.setEnabled(!connectionType.isPredefined());
    }

    @Override
    protected void performDefaults()
    {
        typeTable.removeAll();
        colorPicker.removeAll();
        for (DBPConnectionType connectionType : DBeaverCore.getInstance().getDataSourceProviderRegistry().getConnectionTypes()) {
            TableItem item = new TableItem(typeTable, SWT.LEFT);
            item.setText(0, connectionType.getName());
            item.setText(1, CommonUtils.toString(connectionType.getDescription()));
            if (connectionType.getColor() != null) {
                item.setBackground(0, connectionType.getColor());
                item.setBackground(1, connectionType.getColor());
                colorPicker.add(null, COLOR_TEXT, connectionType.getColor(), connectionType.getColor());
            }
            item.setData(connectionType);
        }
        typeTable.select(0);
        // Ad predefined colors
        int[] colorList = { SWT.COLOR_WHITE, SWT.COLOR_BLACK, SWT.COLOR_RED, SWT.COLOR_DARK_RED,
        SWT.COLOR_GREEN, SWT.COLOR_DARK_GREEN, SWT.COLOR_YELLOW, SWT.COLOR_DARK_YELLOW,
        SWT.COLOR_BLUE, SWT.COLOR_DARK_BLUE, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_MAGENTA,
        SWT.COLOR_CYAN, SWT.COLOR_DARK_CYAN, SWT.COLOR_GRAY, SWT.COLOR_DARK_GRAY };
        for (int colorCode : colorList) {
            Color color = colorPicker.getShell().getDisplay().getSystemColor(colorCode);
            colorPicker.add(null, COLOR_TEXT, color, color);
        }
        selectType(getSelectedType());

        UIUtils.packColumns(typeTable);
        super.performDefaults();
    }

    private DBPConnectionType getSelectedType()
    {
        return (DBPConnectionType) typeTable.getItem(typeTable.getSelectionIndex()).getData();
    }

    @Override
    public boolean performOk()
    {
        return super.performOk();
    }

    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {

    }

}