/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.dashboard.*;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardRendererType;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DashboardItemConfigurationDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardEditDialog";//$NON-NLS-1$

    private final DashboardItemConfiguration itemDescriptor;

    private Text idText;
    private Text nameText;
    private Text descriptionText;
    private Text queryText;

    private Combo viewTypeCombo;
    private Text updatePeriodText;
    private Text maxItemsText;
    //private Text maxAgeText;
    private List<DashboardRendererType> viewTypes;
    private Combo dataTypeCombo;
    private Combo calcTypeCombo;
    private Combo valueTypeCombo;
    private Combo intervalCombo;
    private Combo fetchTypeCombo;

    private DBPNamedObject targetDatabase;

    public DashboardItemConfigurationDialog(Shell shell, DashboardItemConfiguration itemDescriptor) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_edit_dashboard_title, itemDescriptor.getName()), null);

        this.itemDescriptor = itemDescriptor;

        List<DBPNamedObject> dataSourceMappings = itemDescriptor.getDataSourceMappings();
        if (!dataSourceMappings.isEmpty()) {
            targetDatabase = dataSourceMappings.get(0);
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        boolean readOnly = !itemDescriptor.isCustom();
        int baseStyle = !readOnly ? SWT.NONE : SWT.READ_ONLY;

        if (readOnly) {
            UIUtils.createInfoLabel(composite, UIDashboardMessages.dialog_edit_dashboard_infolabels_predifined_dashboard);
        }

        DashboardRendererDescriptor renderer = DashboardUIRegistry.getInstance().getViewType(itemDescriptor.getDashboardRenderer());
        boolean isNativeItem = renderer != null && renderer.isNativeRenderer();

        {
            Group infoGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_edit_dashboard_maininfo, 4, GridData.FILL_HORIZONTAL, 0);

            idText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_id, itemDescriptor.getId(), SWT.BORDER | baseStyle);
            idText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            idText.addModifyListener(e -> updateButtons());
            nameText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_name, itemDescriptor.getName(), SWT.BORDER | baseStyle);
            nameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            nameText.addModifyListener(e -> updateButtons());

            if (isNativeItem) {
                UIUtils.createControlLabel(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_db);
                Composite dbSelectorPanel = UIUtils.createComposite(infoGroup, 2);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 3;
                dbSelectorPanel.setLayoutData(gd);
                Text dbSelectorText = new Text(dbSelectorPanel, SWT.READ_ONLY | SWT.BORDER);
                dbSelectorText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                if (targetDatabase != null) {
                    dbSelectorText.setText(targetDatabase.getName());
                }
                UIUtils.createPushButton(dbSelectorPanel, UIDashboardMessages.dialog_edit_dashboard_maininfo_buttons_select, null, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DashboardDatabaseSelectDialog selectDialog = new DashboardDatabaseSelectDialog(getShell());
                        if (selectDialog.open() == IDialogConstants.OK_ID) {
                            targetDatabase = selectDialog.getTarget();
                            dbSelectorText.setText(targetDatabase.getName());
                        }
                        updateButtons();
                    }
                });
            }


            if (isNativeItem) {
                dataTypeCombo = UIUtils.createLabelCombo(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_datatype, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_datatype_tooltip, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                for (DBDashboardDataType ddt : DBDashboardDataType.values()) {
                    dataTypeCombo.add(ddt.name());
                }
                dataTypeCombo.setText(itemDescriptor.getDataType().name());
                dataTypeCombo.setEnabled(!readOnly);

                calcTypeCombo = UIUtils.createLabelCombo(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_calctype, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_calctype_tooltip, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                for (DBDashboardCalcType dct : DBDashboardCalcType.values()) {
                    calcTypeCombo.add(dct.name());
                }
                calcTypeCombo.setText(itemDescriptor.getCalcType().name());
                calcTypeCombo.setEnabled(!readOnly);

                valueTypeCombo = UIUtils.createLabelCombo(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_valuetype, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_valuetype_tooltip, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                for (DBDashboardValueType dvt : DBDashboardValueType.values()) {
                    valueTypeCombo.add(dvt.name());
                }
                valueTypeCombo.setText(itemDescriptor.getValueType().name());
                valueTypeCombo.setEnabled(!readOnly);

                intervalCombo = UIUtils.createLabelCombo(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_interval, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_interval_tooltip, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                for (DBDashboardInterval dvt : DBDashboardInterval.values()) {
                    intervalCombo.add(dvt.name());
                }
                intervalCombo.setText(itemDescriptor.getInterval().name());
                intervalCombo.setEnabled(!readOnly);

                fetchTypeCombo = UIUtils.createLabelCombo(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_fetchtype, UIDashboardMessages.dialog_edit_dashboard_maininfo_combos_fetchtype_tooltip, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                for (DBDashboardFetchType dft : DBDashboardFetchType.values()) {
                    fetchTypeCombo.add(dft.name());
                }
                fetchTypeCombo.setText(itemDescriptor.getFetchType().name());
                fetchTypeCombo.setEnabled(!readOnly);

                UIUtils.createEmptyLabel(infoGroup, 2, 1);

                descriptionText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_description, CommonUtils.notEmpty(itemDescriptor.getDescription()), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | baseStyle);
                ((GridData) descriptionText.getLayoutData()).heightHint = 30;
                ((GridData) descriptionText.getLayoutData()).widthHint = 300;
                ((GridData) descriptionText.getLayoutData()).horizontalSpan = 3;
            }
        }

        if (isNativeItem) {
            Group sqlGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_edit_dashboard_queries, 1, GridData.FILL_BOTH, 0);
            queryText = new Text(sqlGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | baseStyle);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 100;
            gd.widthHint = 400;
            queryText.setLayoutData(gd);
            UIUtils.createInfoLabel(sqlGroup, UIDashboardMessages.dialog_edit_dashboard_queries_infolabels_separator);

            String lineSeparator = GeneralUtils.getDefaultLineSeparator();

            StringBuilder sql = new StringBuilder();
            for (DashboardItemConfiguration.QueryMapping query : itemDescriptor.getQueries()) {
                sql.append(query.getQueryText().trim()).append(lineSeparator).append(lineSeparator);
            }
            if (itemDescriptor.getMapQuery() != null) {
                sql.append(itemDescriptor.getMapQuery().getQueryText()).append(lineSeparator).append(lineSeparator);
                if (!ArrayUtils.isEmpty(itemDescriptor.getMapKeys())) {
                    sql.append(UIDashboardMessages.dialog_edit_dashboard_queries_keys).append(" ").append(Arrays.toString(itemDescriptor.getMapKeys())).append(lineSeparator);
                }
                if (!ArrayUtils.isEmpty(itemDescriptor.getMapLabels())) {
                    sql.append(UIDashboardMessages.dialog_edit_dashboard_queries_labels).append(" ").append(Arrays.toString(itemDescriptor.getMapLabels())).append(lineSeparator);
                }
            }
            queryText.setText(sql.toString().trim());
            queryText.addModifyListener(e -> updateButtons());
        }

        if (isNativeItem) {
            Group updateGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_edit_dashboard_rendering, 2, GridData.FILL_HORIZONTAL, 0);

            viewTypeCombo = UIUtils.createLabelCombo(updateGroup, UIDashboardMessages.dialog_edit_dashboard_rendering_combos_defaultview, UIDashboardMessages.dialog_edit_dashboard_rendering_combos_defaultview_tooltip, SWT.BORDER | SWT.READ_ONLY);
            viewTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            {
                viewTypes = DashboardUIRegistry.getInstance().getAllViewTypes();
                for (DashboardRendererType viewType : viewTypes) {
                    viewTypeCombo.add(viewType.getTitle());
                }
                DashboardRendererDescriptor viewType = DashboardUIRegistry.getInstance().getViewType(itemDescriptor.getDashboardRenderer());
                viewTypeCombo.setText(viewType.getTitle());
                if (viewTypeCombo.getSelectionIndex() < 0) {
                    viewTypeCombo.select(0);
                }
            }
            viewTypeCombo.setEnabled(!readOnly);

            updatePeriodText = UIUtils.createLabelText(updateGroup, UIDashboardMessages.dialog_edit_dashboard_rendering_labels_updateperiod, String.valueOf(itemDescriptor.getUpdatePeriod()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
            maxItemsText = UIUtils.createLabelText(updateGroup, UIDashboardMessages.dialog_edit_dashboard_rendering_labels_maxitems, String.valueOf(itemDescriptor.getMaxItems()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
            //maxAgeText = UIUtils.createLabelText(updateGroup, "Maximum age (ISO-8601)", DashboardUtils.formatDuration(dashboardDescriptor.getMaxAge()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        updateButtons();

        return contents;
    }

    private void updateButtons() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(
            itemDescriptor.isCustom() &&
            !idText.getText().isEmpty() &&
            !nameText.getText().isEmpty() &&
            (queryText == null || !queryText.getText().isEmpty()) &&
            (viewTypeCombo == null || viewTypeCombo.getSelectionIndex() >= 0) &&
            targetDatabase != null
        );
    }


    private void saveSettings() {
        itemDescriptor.setId(idText.getText());
        itemDescriptor.setName(nameText.getText());
        itemDescriptor.setDataSourceMappings(Collections.singletonList(targetDatabase));
        itemDescriptor.setDescription(descriptionText.getText());
        itemDescriptor.setDataType(DBDashboardDataType.values()[dataTypeCombo.getSelectionIndex()]);
        itemDescriptor.setCalcType(DBDashboardCalcType.values()[calcTypeCombo.getSelectionIndex()]);
        itemDescriptor.setValueType(DBDashboardValueType.values()[valueTypeCombo.getSelectionIndex()]);
        itemDescriptor.setInterval(DBDashboardInterval.values()[intervalCombo.getSelectionIndex()]);
        itemDescriptor.setFetchType(DBDashboardFetchType.values()[fetchTypeCombo.getSelectionIndex()]);
        itemDescriptor.setQueries(queryText.getText().split("\\n\\s*\\n"));

        itemDescriptor.setRenderer(viewTypes.get(viewTypeCombo.getSelectionIndex()).getId());
        itemDescriptor.setUpdatePeriod(CommonUtils.toLong(updatePeriodText.getText(), itemDescriptor.getUpdatePeriod()));
        itemDescriptor.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), itemDescriptor.getMaxItems()));
        //dashboardDescriptor.setMaxAge(DashboardUtils.parseDuration(maxAgeText.getText(), dashboardDescriptor.getMaxAge()));
    }

    @Override
    protected void okPressed() {
        saveSettings();
        DashboardRegistry.getInstance().saveSettings();
        super.okPressed();
    }

}