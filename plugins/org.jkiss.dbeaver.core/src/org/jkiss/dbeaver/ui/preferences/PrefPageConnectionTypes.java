/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * PrefPageConnectionTypes
 */
public class PrefPageConnectionTypes extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.connectionTypes"; //$NON-NLS-1$

    private Table typeTable;
    private Text typeName;
    private Text typeDescription;
    private ColorSelector colorPicker;
    private Button autocommitCheck;
    private Button confirmCheck;
    private ToolItem deleteButton;
    private DBPConnectionType selectedType;

    private Map<DBPConnectionType, DBPConnectionType> changedInfo = new HashMap<>();

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


            ToolBar toolbar = new ToolBar(composite, SWT.FLAT | SWT.HORIZONTAL);
            final ToolItem newButton = new ToolItem(toolbar, SWT.NONE);
            newButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_ADD));
            deleteButton = new ToolItem(toolbar, SWT.NONE);
            deleteButton.setImage(DBeaverIcons.getImage(UIIcon.ROW_DELETE));

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

            this.deleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBPConnectionType connectionType = getSelectedType();
                    if (!UIUtils.confirmAction(
                        getShell(),
                        "Delete connection type", "Are you sure you want to delete connection type '" + connectionType.getName() + "'?\n" +
                            "All connections of this type will be reset to default type (" + DBPConnectionType.DEFAULT_TYPE.getName() + ")")) {
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
//                Composite colorGroup = UIUtils.createPlaceholder(groupSettings, 2, 5);
//                colorGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                colorPicker = new ColorSelector(groupSettings);
//                colorPicker.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                colorPicker.addListener(new IPropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent event) {
                        getSelectedType().setColor(StringConverter.asString(colorPicker.getColorValue()));
                        updateTableInfo();
                    }
                });
/*
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
                                Color item = colorPicker.getColorItem(i);
                                if (item != null && item.getRGB().equals(rgb)) {
                                    color = item;
                                    break;
                                }
                            }
                            if (color == null) {
                                color = new Color(colorPicker.getDisplay(), rgb);
                                colorPicker.addColor(color);
                            }
                            colorPicker.select(color);
                            getSelectedType().setColor(StringConverter.asString(color.getRGB()));
                            updateTableInfo();
                        }
                    }
                });
*/
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
        final Color connectionTypeColor = UIUtils.getConnectionTypeColor(connectionType);
        if (connectionTypeColor != null) {
            colorPicker.setColorValue(connectionTypeColor.getRGB());
        }

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
        //colorPicker.loadStandardColors();
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
            if (connectionColor != null) {
                colorPicker.setColorValue(connectionColor.getRGB());
            }
        }
        item.setData(connectionType);
    }

    @Override
    public boolean performOk()
    {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        java.util.List<DBPConnectionType> toRemove = new ArrayList<>();
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
        selectedType = element.getAdapter(DBPConnectionType.class);
    }

}
