/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionPermissionsDialog;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.List;
import java.util.*;

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
    private Button confirmDataChange;
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
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            typeTable = new Table(composite, SWT.SINGLE | SWT.BORDER);
            typeTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createTableColumn(typeTable, SWT.LEFT, CoreMessages.pref_page_connection_types_label_table_column_name);
            UIUtils.createTableColumn(typeTable, SWT.LEFT, CoreMessages.pref_page_connection_types_label_table_column_description);
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
                        false,
                        true);
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
                        CoreMessages.pref_page_connection_types_label_delete_connection_type, NLS.bind(CoreMessages.pref_page_connection_types_label_delete_connection_type_description, 
                        		connectionType.getName() , DBPConnectionType.DEFAULT_TYPE.getName()))) {
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
            Group groupSettings = UIUtils.createControlGroup(composite, CoreMessages.pref_page_connection_types_group_settings, 2, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            groupSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            typeName = UIUtils.createLabelText(groupSettings, CoreMessages.pref_page_connection_types_label_name, null);
            typeName.addModifyListener(e -> {
                getSelectedType().setName(typeName.getText());
                updateTableInfo();

            });
            typeDescription = UIUtils.createLabelText(groupSettings, CoreMessages.pref_page_connection_types_label_description, null);
            typeDescription.addModifyListener(e -> {
                getSelectedType().setDescription(typeDescription.getText());
                updateTableInfo();
            });

            {
                UIUtils.createControlLabel(groupSettings, CoreMessages.pref_page_connection_types_label_color);
//                Composite colorGroup = UIUtils.createPlaceholder(groupSettings, 2, 5);
//                colorGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                colorPicker = new ColorSelector(groupSettings);
//                colorPicker.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                colorPicker.addListener(event -> {
                    getSelectedType().setColor(StringConverter.asString(colorPicker.getColorValue()));
                    updateTableInfo();
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

            autocommitCheck = UIUtils.createCheckbox(groupSettings, CoreMessages.pref_page_connection_types_label_auto_commit_by_default, null, false, 2);
            autocommitCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getSelectedType().setAutocommit(autocommitCheck.getSelection());
                }
            });
            confirmCheck = UIUtils.createCheckbox(groupSettings, CoreMessages. pref_page_connection_types_label_confirm_sql_execution, null, false, 2);
            confirmCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getSelectedType().setConfirmExecute(confirmCheck.getSelection());
                }
            });

            confirmDataChange = UIUtils.createCheckbox(groupSettings, CoreMessages.pref_page_connection_types_label_confirm_data_change, CoreMessages.pref_page_connection_types_label_confirm_data_change_tip, false, 2);
            confirmDataChange.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    getSelectedType().setConfirmDataChange(confirmDataChange.getSelection());
                }
            });

            Button epButton = UIUtils.createDialogButton(groupSettings, "Edit permissions ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    EditConnectionPermissionsDialog dialog = new EditConnectionPermissionsDialog(getShell(), getSelectedType().getModifyPermission());
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        getSelectedType().setModifyPermissions(dialog.getAccessRestrictions());
                    }
                }
            });
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            epButton.setLayoutData(gd);
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
        } else {
            colorPicker.setColorValue(colorPicker.getButton().getBackground().getRGB());
        }

        typeName.setText(connectionType.getName());
        typeDescription.setText(connectionType.getDescription());
        autocommitCheck.setSelection(connectionType.isAutocommit());
        confirmCheck.setSelection(connectionType.isConfirmExecute());
        confirmDataChange.setSelection(connectionType.isConfirmDataChange());

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
                //item.setBackground(0, connectionColor);
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
            //item.setBackground(0, connectionColor);
            item.setBackground(1, connectionColor);
            if (connectionColor != null) {
                //colorPicker.setColorValue(connectionColor.getRGB());
            }
        }
        item.setData(connectionType);
    }

    @Override
    public boolean performOk()
    {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        List<DBPConnectionType> toRemove = new ArrayList<>();
        for (DBPConnectionType type : registry.getConnectionTypes()) {
            if (!changedInfo.values().contains(type)) {
                // Remove
                toRemove.add(type);
            }
        }

        Set<DBPConnectionType> changedSet = new HashSet<>();

        for (DBPConnectionType connectionType : toRemove) {
            registry.removeConnectionType(connectionType);
            changedSet.add(connectionType);
        }

        for (DBPConnectionType changed : changedInfo.keySet()) {
            boolean hasChanges = false;
            DBPConnectionType source = changedInfo.get(changed);
            if (source == changed) {
                // New type
                registry.addConnectionType(changed);
                hasChanges = true;
            } else if (!source.equals(changed)) {
                // Changed type
                source.setName(changed.getName());
                source.setDescription(changed.getDescription());
                source.setAutocommit(changed.isAutocommit());
                source.setConfirmExecute(changed.isConfirmExecute());
                source.setConfirmDataChange(changed.isConfirmDataChange());
                source.setColor(changed.getColor());
                source.setModifyPermissions(changed.getModifyPermission());
                hasChanges = true;
            }
            if (hasChanges) {
                changedSet.add(source);
            }
        }

        if (!changedSet.isEmpty()) {
            registry.saveConnectionTypes();
            // Flush projects configs (as they cache connection type information)
            for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
                DBPDataSourceRegistry projectRegistry = project.getDataSourceRegistry();
                for (DBPDataSourceContainer ds : projectRegistry.getDataSources()) {
                    if (changedSet.contains(ds.getConnectionConfiguration().getConnectionType())) {
                        projectRegistry.flushConfig();
                        break;
                    }
                }
            }
        }
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
