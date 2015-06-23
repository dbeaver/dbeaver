/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.model.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * PrefPageConnectionTypes
 */
public class PrefPageConnectionTypes extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.connectionTypes"; //$NON-NLS-1$
    public static final String COLOR_TEXT = "";

    private Table typeTable;
    private Text typeName;
    private Text typeDescription;
    private CImageCombo colorPicker;
    private Button autocommitCheck;
    private Button confirmCheck;
    private Button deleteButton;
    private DBPConnectionType selectedType;

    private Map<DBPConnectionType, DBPConnectionType> changedInfo = new HashMap<DBPConnectionType, DBPConnectionType>();

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
                    showSelectedType(getSelectedType());
                }
            });

            Composite tableGroup = UIUtils.createPlaceholder(composite, 2, 5);
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Button newButton = new Button(tableGroup, SWT.PUSH);
            newButton.setText("New");
            newButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    String name;
                    for (int i = 1;; i++) {
                        name = "Type" + i;
                        boolean hasName = false;
                        for (DBPConnectionType type : changedInfo.keySet()) {
                            if (type.getName().equals(name)) {
                                hasName = true;
                                break;
                            }
                        }
                        if (!hasName) {
                            break;
                        }
                    }
                    DBPConnectionType newType = new DBPConnectionType(
                        SecurityUtils.generateUniqueId(),
                        name,
                        "255,255,255",
                        "New type",
                        true,
                        false);
                    addTypeToTable(newType, newType);
                    typeTable.select(typeTable.getItemCount() - 1);
                    typeTable.showSelection();
                    showSelectedType(newType);
                }
            });

            deleteButton = new Button(tableGroup, SWT.PUSH);
            deleteButton.setText("Delete");
            deleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DBPConnectionType connectionType = getSelectedType();
                    if (!UIUtils.confirmAction(
                        deleteButton.getShell(),
                        "Delete connection type", "Are you sure you want to delete connection type '" + connectionType.getName() + "'?\n" +
                        "All connections of this type will be reset to default type (" + DBPConnectionType.DEFAULT_TYPE.getName() + ")"))
                    {
                        return;
                    }
                    changedInfo.remove(connectionType);
                    int index = typeTable.getSelectionIndex();
                    typeTable.remove(index);
                    if (index > 0) index--;
                    typeTable.select(index);
                    showSelectedType(getSelectedType());
                }
            });
        }

        {
            Group groupSettings = UIUtils.createControlGroup(composite, "Settings", 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            groupSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            typeName = UIUtils.createLabelText(groupSettings, "Name", null);
            typeName.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    getSelectedType().setName(typeName.getText());
                    updateTableInfo();

                }
            });
            typeDescription = UIUtils.createLabelText(groupSettings, "Description", null);
            typeDescription.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    getSelectedType().setDescription(typeDescription.getText());
                    updateTableInfo();
                }
            });

            {
                UIUtils.createControlLabel(groupSettings, "Color");
                Composite colorGroup = UIUtils.createPlaceholder(groupSettings, 2, 5);
                colorGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                colorPicker = new CImageCombo(colorGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                colorPicker.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                colorPicker.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        getSelectedType().setColor(StringConverter.asString(colorPicker.getItem(colorPicker.getSelectionIndex()).getBackground().getRGB()));
                        updateTableInfo();
                    }
                });
                Button pickerButton = new Button(colorGroup, SWT.PUSH);
                pickerButton.setText("...");
                pickerButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        DBPConnectionType connectionType = getSelectedType();
                        ColorDialog colorDialog = new ColorDialog(parent.getShell());
                        colorDialog.setRGB(StringConverter.asRGB(connectionType.getColor()));
                        RGB rgb = colorDialog.open();
                        if (rgb != null) {
                            Color color = null;
                            int count = colorPicker.getItemCount();
                            for (int i = 0; i < count; i++) {
                                TableItem item = colorPicker.getItem(i);
                                if (item.getBackground() != null && item.getBackground().getRGB().equals(rgb)) {
                                    color = item.getBackground();
                                    break;
                                }
                            }
                            if (color == null) {
                                color = new Color(colorPicker.getDisplay(), rgb);
                                colorPicker.add(null, COLOR_TEXT, color, color);
                            }
                            colorPicker.select(color);
                            getSelectedType().setColor(StringConverter.asString(color.getRGB()));
                            updateTableInfo();
                        }
                    }
                });
            }

            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;

            autocommitCheck = UIUtils.createCheckbox(groupSettings, "Auto-commit by default", false);
            autocommitCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getSelectedType().setAutocommit(autocommitCheck.getSelection());
                }
            });
            autocommitCheck.setLayoutData(gd);
            confirmCheck = UIUtils.createCheckbox(groupSettings, "Confirm SQL execution", false);
            confirmCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getSelectedType().setConfirmExecute(confirmCheck.getSelection());
                }
            });
            confirmCheck.setLayoutData(gd);
        }

        performDefaults();

        return composite;
    }

    private DBPConnectionType getSelectedType()
    {
        return (DBPConnectionType) typeTable.getItem(typeTable.getSelectionIndex()).getData();
    }

    private void showSelectedType(DBPConnectionType connectionType)
    {
        colorPicker.select(UIUtils.getConnectionTypeColor(connectionType));
        typeName.setText(connectionType.getName());
        typeDescription.setText(connectionType.getDescription());
        autocommitCheck.setSelection(connectionType.isAutocommit());
        confirmCheck.setSelection(connectionType.isConfirmExecute());

        deleteButton.setEnabled(!connectionType.isPredefined());
    }

    private void updateTableInfo()
    {
        DBPConnectionType connectionType = getSelectedType();
        for (TableItem item : typeTable.getItems()) {
            if (item.getData() == connectionType) {
                item.setText(0, connectionType.getName());
                item.setText(1, connectionType.getDescription());
                Color connectionColor = UIUtils.getConnectionTypeColor(connectionType);
                item.setBackground(0, connectionColor);
                item.setBackground(1, connectionColor);
                break;
            }
        }
    }

    @Override
    protected void performDefaults()
    {
        typeTable.removeAll();
        colorPicker.removeAll();
        for (DBPConnectionType source : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            addTypeToTable(source, new DBPConnectionType(source));
        }
        typeTable.select(0);
        if (selectedType != null) {
            for (int i = 0; i < typeTable.getItemCount(); i++) {
                if (typeTable.getItem(i).getData().equals(selectedType)) {
                    typeTable.select(i);
                    break;
                }
            }
        }
        // Ad predefined colors
        int[] colorList = { SWT.COLOR_WHITE, SWT.COLOR_BLACK, SWT.COLOR_RED, SWT.COLOR_DARK_RED,
        SWT.COLOR_GREEN, SWT.COLOR_DARK_GREEN, SWT.COLOR_YELLOW, SWT.COLOR_DARK_YELLOW,
        SWT.COLOR_BLUE, SWT.COLOR_DARK_BLUE, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_MAGENTA,
        SWT.COLOR_CYAN, SWT.COLOR_DARK_CYAN, SWT.COLOR_GRAY, SWT.COLOR_DARK_GRAY };
        for (int colorCode : colorList) {
            Color color = colorPicker.getShell().getDisplay().getSystemColor(colorCode);
            colorPicker.add(null, COLOR_TEXT, color, color);
        }
        showSelectedType(getSelectedType());

        typeTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e)
            {
                UIUtils.packColumns(typeTable, true);
            }
        });
        super.performDefaults();
    }

    private void addTypeToTable(DBPConnectionType source, DBPConnectionType connectionType)
    {
        changedInfo.put(connectionType, source);
        TableItem item = new TableItem(typeTable, SWT.LEFT);
        item.setText(0, connectionType.getName());
        item.setText(1, CommonUtils.toString(connectionType.getDescription()));
        if (connectionType.getColor() != null) {
            Color connectionColor = UIUtils.getConnectionTypeColor(connectionType);
            item.setBackground(0, connectionColor);
            item.setBackground(1, connectionColor);
            colorPicker.add(null, COLOR_TEXT, connectionColor, connectionColor);
        }
        item.setData(connectionType);
    }

    @Override
    public boolean performOk()
    {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        java.util.List<DBPConnectionType> toRemove = new ArrayList<DBPConnectionType>();
        for (DBPConnectionType type : registry.getConnectionTypes()) {
            if (!changedInfo.values().contains(type)) {
                // Remove
                toRemove.add(type);
            }
        }
        for (DBPConnectionType connectionType : toRemove) {
            registry.removeConnectionType(connectionType);
        }

        for (DBPConnectionType changed : changedInfo.keySet()) {
            DBPConnectionType source = changedInfo.get(changed);
            if (source == changed) {
                // New type
                registry.addConnectionType(changed);
            } else {
                // Changed type
                source.setName(changed.getName());
                source.setDescription(changed.getDescription());
                source.setAutocommit(changed.isAutocommit());
                source.setConfirmExecute(changed.isConfirmExecute());
                source.setColor(changed.getColor());
            }
        }
        registry.saveConnectionTypes();
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
        selectedType = (DBPConnectionType) element.getAdapter(DBPConnectionType.class);
    }

}
