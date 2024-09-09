/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionPermissionsDialog;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.*;

/**
 * PrefPageConnectionTypes
 */
public class PrefPageConnectionTypes extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.connectionTypes"; //$NON-NLS-1$

    private static final String HELP_CONNECTION_TYPES_LINK = "Connection-Types";

    private Table typeTable;
    private Text typeId;
    private Text typeName;
    private Text typeDescription;
    private ColorSelector colorPicker;
    private Button autocommitCheck;
    private Button confirmCheck;
    private Button confirmDataChangeCheck;
    private Button autoCloseTransactionsCheck;
    private Text autoCloseTransactionsTtlText;
    private Button autoCloseConnectionsCheck;
    private Text autoCloseConnectionsTtlText;
    private Button smartCommitCheck;
    private Button smartCommitRecoverCheck;
    private ToolItem deleteButton;
    private DBPConnectionType selectedType;

    private final Map<DBPConnectionType, DBPConnectionType> changedInfo = new HashMap<>();

    @Override
    public void init(IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
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
                public void widgetSelected(SelectionEvent e) {
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
                public void widgetSelected(SelectionEvent e) {
                    String name;
                    for (int i = 1; ; i++) {
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
                    DBPConnectionType newType = new DBPConnectionType(DBPConnectionType.DEFAULT_TYPE);
                    newType.setId(name.toLowerCase());
                    newType.setName("New type");
                    newType.setColor("255,255,255");
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
                            connectionType.getName(), DBPConnectionType.DEFAULT_TYPE.getName()))) {
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
            Group groupSettings = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_connection_types_group_parameters,
                2,
                GridData.VERTICAL_ALIGN_BEGINNING,
                300);
            groupSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            typeId = UIUtils.createLabelText(groupSettings, CoreMessages.pref_page_connection_types_label_id, null);
            typeId.addModifyListener(e -> {
                getSelectedType().setId(typeId.getText());
                updateTableInfo();
            });
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
        }

        {
            Group placeholder = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_connection_types_group_settings,
                2,
                GridData.VERTICAL_ALIGN_BEGINNING,
                300);
            placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));

            confirmCheck = UIUtils.createCheckbox(
                placeholder,
                CoreMessages.pref_page_connection_types_label_confirm_sql_execution,
                CoreMessages.pref_page_connection_types_label_confirm_sql_execution_tip,
                false,
                2);
            confirmCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setConfirmExecute(confirmCheck.getSelection());
                }
            });

            confirmDataChangeCheck = UIUtils.createCheckbox(
                placeholder,
                CoreMessages.pref_page_connection_types_label_confirm_data_change,
                CoreMessages.pref_page_connection_types_label_confirm_data_change_tip,
                false,
                2);
            confirmDataChangeCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setConfirmDataChange(confirmDataChangeCheck.getSelection());
                }
            });

            autocommitCheck = UIUtils.createCheckbox(
                placeholder,
                CoreMessages.pref_page_connection_types_label_auto_commit_by_default,
                CoreMessages.pref_page_connection_types_label_auto_commit_by_default_tip,
                false,
                2);
            autocommitCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setAutocommit(autocommitCheck.getSelection());
                }
            });

            smartCommitCheck = UIUtils.createCheckbox(placeholder,
                CoreMessages.action_menu_transaction_smart_auto_commit,
                CoreMessages.action_menu_transaction_smart_auto_commit_tip,
                false,
                2);
            smartCommitCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setSmartCommit(smartCommitCheck.getSelection());
                    updateCommitRecoverCheckBox();
                }
            });
            smartCommitRecoverCheck = UIUtils.createCheckbox(placeholder,
                CoreMessages.action_menu_transaction_smart_auto_commit_recover,
                CoreMessages.action_menu_transaction_smart_auto_commit_recover_tip,
                false,
                2);
            smartCommitRecoverCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setSmartCommitRecover(smartCommitRecoverCheck.getSelection());
                }
            });
            // transactions
            autoCloseTransactionsCheck = UIUtils.createCheckbox(
                placeholder,
                CoreMessages.action_menu_transaction_auto_close_enabled,
                CoreMessages.pref_page_connection_types_label_auto_close_enabled_tip,
                true,
                1);
            autoCloseTransactionsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setAutoCloseTransactions(autoCloseTransactionsCheck.getSelection());
                }
            });
            autoCloseTransactionsTtlText = new Text(placeholder, SWT.BORDER);
            autoCloseTransactionsTtlText.setToolTipText(CoreMessages.pref_page_connection_types_label_auto_close_ttl_tip);
            autoCloseTransactionsTtlText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
            GridData grd = new GridData();
            grd.widthHint = UIUtils.getFontHeight(autoCloseTransactionsTtlText) * 6;
            autoCloseTransactionsTtlText.setLayoutData(grd);
            autoCloseTransactionsTtlText.addModifyListener(e ->
                getSelectedType().setCloseIdleTransactionPeriod(
                    CommonUtils.toInt(autoCloseTransactionsTtlText.getText(), DBPConnectionType.DEFAULT_TYPE.getCloseIdleTransactionPeriod())));
            // connections
            autoCloseConnectionsCheck = UIUtils.createCheckbox(
                placeholder,
                CoreMessages.dialog_connection_wizard_final_label_close_idle_connections,
                CoreMessages.dialog_connection_wizard_final_label_close_idle_connections_tooltip,
                true,
                1);
            autoCloseConnectionsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getSelectedType().setAutoCloseConnections(autoCloseConnectionsCheck.getSelection());
                }
            });
            autoCloseConnectionsTtlText = new Text(placeholder, SWT.BORDER);
            autoCloseConnectionsTtlText.setToolTipText(CoreMessages.pref_page_connection_types_label_auto_close_ttl_tip);
            autoCloseConnectionsTtlText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
            GridData grdConnections = new GridData();
            grdConnections.widthHint = UIUtils.getFontHeight(autoCloseTransactionsTtlText) * 6;
            autoCloseConnectionsTtlText.setLayoutData(grdConnections);
            autoCloseConnectionsTtlText.addModifyListener(e ->
                getSelectedType().setCloseIdleConnectionPeriod(
                    CommonUtils.toInt(autoCloseConnectionsTtlText.getText(), DBPConnectionType.DEFAULT_TYPE.getCloseIdleConnectionPeriod())));

            Button epButton = UIUtils.createDialogButton(
                placeholder,
                CoreMessages.pref_page_label_edit_permissions,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditConnectionPermissionsDialog dialog = new EditConnectionPermissionsDialog(
                            getShell(), getSelectedType().getModifyPermission()
                        );
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            getSelectedType().setModifyPermissions(dialog.getAccessRestrictions());
                        }
                    }
                }
            );
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            epButton.setLayoutData(gd);
        }

        Link urlHelpLabel = UIUtils.createLink(
            composite,
            "<a>" + CoreMessages.pref_page_connection_types_wiki_link + "</a>",
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(HELP_CONNECTION_TYPES_LINK));
                }
            });
        GridData gridData = new GridData(GridData.FILL, SWT.END, true, true);
        urlHelpLabel.setLayoutData(gridData);

        performDefaults(false);
        updateCommitRecoverCheckBox();

        return composite;
    }

    private void updateCommitRecoverCheckBox() {
        if (!smartCommitCheck.getSelection()) {
            smartCommitRecoverCheck.setEnabled(false);
        } else if (!smartCommitRecoverCheck.isEnabled()) {
            smartCommitRecoverCheck.setEnabled(true);
        }
    }

    private DBPConnectionType getSelectedType() {
        return (DBPConnectionType) typeTable.getItem(typeTable.getSelectionIndex()).getData();
    }

    private void showSelectedType(DBPConnectionType connectionType) {
        final Color connectionTypeColor = UIUtils.getConnectionTypeColor(connectionType);
        if (connectionTypeColor != null) {
            colorPicker.setColorValue(connectionTypeColor.getRGB());
        } else {
            colorPicker.setColorValue(colorPicker.getButton().getBackground().getRGB());
        }

        typeId.setText(connectionType.getId());
        typeId.setEnabled(changedInfo.get(connectionType) == connectionType);
        typeName.setText(connectionType.getName());
        typeDescription.setText(connectionType.getDescription());
        autocommitCheck.setSelection(connectionType.isAutocommit());
        confirmCheck.setSelection(connectionType.isConfirmExecute());
        confirmDataChangeCheck.setSelection(connectionType.isConfirmDataChange());
        smartCommitCheck.setSelection(connectionType.isSmartCommit());
        smartCommitRecoverCheck.setSelection(connectionType.isSmartCommitRecover());
        autoCloseTransactionsCheck.setSelection(connectionType.isAutoCloseTransactions());
        autoCloseTransactionsTtlText.setText(String.valueOf(connectionType.getCloseIdleTransactionPeriod()));
        autoCloseConnectionsCheck.setSelection(connectionType.isAutoCloseConnections());
        autoCloseConnectionsTtlText.setText(String.valueOf(connectionType.getCloseIdleConnectionPeriod()));
        deleteButton.setEnabled(!connectionType.isPredefined());
    }

    private void updateTableInfo() {
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

    private static DBPConnectionType findSystemType(DBPConnectionType type) {
        for (DBPConnectionType ct : DBPConnectionType.SYSTEM_TYPES) {
            if (ct.getId().equals(type.getId())) {
                return ct;
            }
        }
        return null;
    }

    @Override
    protected void performDefaults() {
        performDefaults(true);
        super.performDefaults();
    }

    protected void performDefaults(boolean resetSystemSettings) {
        typeTable.removeAll();

        for (DBPConnectionType source : DataSourceProviderRegistry.getInstance().getConnectionTypes()) {
            DBPConnectionType systemType = resetSystemSettings ? findSystemType(source) : null;
            DBPConnectionType connectionType;
            if (systemType != null) {
                connectionType = systemType;
            } else {
                connectionType = new DBPConnectionType(source);
            }
            addTypeToTable(source, connectionType);
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
            public void controlResized(ControlEvent e) {
                typeTable.removeControlListener(this);
                UIUtils.packColumns(typeTable, true);
            }
        });
    }

    private void addTypeToTable(DBPConnectionType source, DBPConnectionType connectionType) {
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
    public boolean performOk() {
        typeId.setEnabled(false);

        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        Set<DBPConnectionType> toRemove = new HashSet<>();
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

        for (Map.Entry<DBPConnectionType, DBPConnectionType> entry : changedInfo.entrySet()) {
            boolean hasChanges = false;
            DBPConnectionType changed = entry.getKey();
            DBPConnectionType source = entry.getValue();
            if (source == changed) {
                // New type
                if (CommonUtils.isEmpty(changed.getId())) {
                    changed.setId(SecurityUtils.generateUniqueId());
                }
                for (DBPConnectionType type : changedInfo.keySet()) {
                    if (type != changed && type.getId().equals(changed.getId())) {
                        changed.setId(SecurityUtils.generateUniqueId());
                        break;
                    }
                }
                entry.setValue(new DBPConnectionType(source));
                registry.addConnectionType(changed);
                hasChanges = true;
            } else if (!source.equals(changed)) {
                // Changed type
                source.setId(changed.getId());
                source.setName(changed.getName());
                source.setDescription(changed.getDescription());
                source.setAutocommit(changed.isAutocommit());
                source.setConfirmExecute(changed.isConfirmExecute());
                source.setConfirmDataChange(changed.isConfirmDataChange());
                source.setColor(changed.getColor());
                source.setModifyPermissions(changed.getModifyPermission());
                source.setSmartCommit(changed.isSmartCommit());
                source.setSmartCommitRecover(changed.isSmartCommitRecover());
                // transaction
                source.setAutoCloseTransactions(changed.isAutoCloseTransactions());
                source.setCloseIdleTransactionPeriod(changed.getCloseIdleTransactionPeriod());
                // connections
                source.setAutoCloseConnections(changed.isAutoCloseConnections());
                source.setCloseIdleConnectionPeriod(changed.getCloseIdleConnectionPeriod());
                hasChanges = true;
            }
            if (hasChanges) {
                changedSet.add(source);
            }
        }

        Set<DBPDataSourceRegistry> affectedDataSourceRegs = new HashSet<>();
        if (!changedSet.isEmpty()) {
            registry.saveConnectionTypes();
            // Flush projects configs (as they cache connection type information)
            for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
                DBPDataSourceRegistry projectRegistry = project.getDataSourceRegistry();
                for (DBPDataSourceContainer ds : projectRegistry.getDataSources()) {
                    DBPConnectionConfiguration cnnCfg = ds.getConnectionConfiguration();
                    DBPConnectionType cnnType = cnnCfg.getConnectionType();
                    if (changedSet.contains(cnnType)) {
                        if (toRemove.contains(cnnType)) {
                            cnnCfg.setConnectionType(DBPConnectionType.DEFAULT_TYPE);
                        }
                        projectRegistry.flushConfig();
                        affectedDataSourceRegs.add(projectRegistry);
                        break;
                    }
                }
            }
        }
        for (DBPDataSourceRegistry dsReg : affectedDataSourceRegs) {
            dsReg.notifyDataSourceListeners(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, null, dsReg));
        }
        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {
        selectedType = element.getAdapter(DBPConnectionType.class);
    }

}
