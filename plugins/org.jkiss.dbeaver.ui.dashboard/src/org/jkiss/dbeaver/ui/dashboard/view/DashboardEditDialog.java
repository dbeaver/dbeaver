/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardViewTypeDescriptor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DashboardEditDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardEditDialog";//$NON-NLS-1$

    private final DashboardDescriptor dashboardDescriptor;

    private Text idText;
    private Text nameText;
    private Text descriptionText;
    private Text queryText;

    private Combo viewTypeCombo;
    private Text updatePeriodText;
    private Text maxItemsText;
    private Text maxAgeText;
    private List<DashboardViewType> viewTypes;
    private Combo dataTypeCombo;
    private Combo calcTypeCombo;
    private Combo valueTypeCombo;
    private Combo fetchTypeCombo;

    public DashboardEditDialog(Shell shell, DashboardDescriptor dashboardDescriptor)
    {
        super(shell, "Dashboard [" + dashboardDescriptor.getName() + "]", null);

        this.dashboardDescriptor = dashboardDescriptor;
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

        boolean readOnly = !dashboardDescriptor.isCustom();
        int baseStyle = !readOnly ? SWT.NONE : SWT.READ_ONLY;

        {
            Group infoGroup = UIUtils.createControlGroup(composite, "Main info", 4, GridData.FILL_HORIZONTAL, 0);

            idText = UIUtils.createLabelText(infoGroup, "ID", dashboardDescriptor.getId(), SWT.BORDER | baseStyle);
            idText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            nameText = UIUtils.createLabelText(infoGroup, "Name", dashboardDescriptor.getName(), SWT.BORDER | baseStyle);
            nameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
//            UIUtils.createLabelText(infoGroup, "Group", CommonUtils.notEmpty(dashboardDescriptor.getGroup()), SWT.BORDER | baseStyle)
//                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            dataTypeCombo = UIUtils.createLabelCombo(infoGroup, "Data type", "Type of data for this dashboard", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DashboardDataType ddt : DashboardDataType.values()) {
                dataTypeCombo.add(ddt.name());
            }
            dataTypeCombo.setText(dashboardDescriptor.getDataType().name());
            dataTypeCombo.setEnabled(!readOnly);

            calcTypeCombo = UIUtils.createLabelCombo(infoGroup, "Calc type", "Value calculation type", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DashboardCalcType dct : DashboardCalcType.values()) {
                calcTypeCombo.add(dct.name());
            }
            calcTypeCombo.setText(dashboardDescriptor.getCalcType().name());
            calcTypeCombo.setEnabled(!readOnly);

            valueTypeCombo = UIUtils.createLabelCombo(infoGroup, "Value type", "Type of values", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DashboardValueType dvt : DashboardValueType.values()) {
                valueTypeCombo.add(dvt.name());
            }
            valueTypeCombo.setText(dashboardDescriptor.getValueType().name());
            valueTypeCombo.setEnabled(!readOnly);

            fetchTypeCombo = UIUtils.createLabelCombo(infoGroup, "Fetch type", "Values fetch type", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DashboardFetchType dft : DashboardFetchType.values()) {
                fetchTypeCombo.add(dft.name());
            }
            fetchTypeCombo.setText(dashboardDescriptor.getFetchType().name());
            fetchTypeCombo.setEnabled(!readOnly);

            descriptionText = UIUtils.createLabelText(infoGroup, "Description", CommonUtils.notEmpty(dashboardDescriptor.getDescription()), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | baseStyle);
            ((GridData) descriptionText.getLayoutData()).heightHint = 30;
            ((GridData) descriptionText.getLayoutData()).widthHint = 300;
            ((GridData) descriptionText.getLayoutData()).horizontalSpan = 3;
        }

        {
            Group sqlGroup = UIUtils.createControlGroup(composite, "Queries", 1, GridData.FILL_HORIZONTAL, 0);
            queryText = new Text(sqlGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | baseStyle);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 100;
            gd.widthHint = 400;
            queryText.setLayoutData(gd);
            UIUtils.createInfoLabel(sqlGroup, "Use blank line as query separator");

            StringBuilder sql = new StringBuilder();
            for (DashboardDescriptor.QueryMapping query : dashboardDescriptor.getQueries()) {
                sql.append(query.getQueryText().trim()).append("\n\n");
            }
            queryText.setText(sql.toString());
        }

        {
            Group updateGroup = UIUtils.createControlGroup(composite, "Rendering", 2, GridData.FILL_HORIZONTAL, 0);

            viewTypeCombo = UIUtils.createLabelCombo(updateGroup, "Default view", "Dashboard view", SWT.BORDER | SWT.READ_ONLY);
            viewTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            {
                viewTypes = DashboardRegistry.getInstance().getSupportedViewTypes(dashboardDescriptor.getDataType());
                for (DashboardViewType viewType : viewTypes) {
                    viewTypeCombo.add(viewType.getTitle());
                }
                viewTypeCombo.setText(dashboardDescriptor.getDefaultViewType().getTitle());
            }
            viewTypeCombo.setEnabled(!readOnly);

            updatePeriodText = UIUtils.createLabelText(updateGroup, "Update period (ms)", String.valueOf(dashboardDescriptor.getUpdatePeriod()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
//            updatePeriodText.addModifyListener(e -> {
//                dashboardDescriptor.setUpdatePeriod(CommonUtils.toLong(updatePeriodText.getText(), dashboardDescriptor.getUpdatePeriod()));
//            });
            maxItemsText = UIUtils.createLabelText(updateGroup, "Maximum items", String.valueOf(dashboardDescriptor.getMaxItems()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
//            maxItemsText.addModifyListener(e -> {
//                dashboardDescriptor.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), dashboardDescriptor.getMaxItems()));
//            });
            maxAgeText = UIUtils.createLabelText(updateGroup, "Maximum age (ISO-8601)", DashboardUtils.formatDuration(dashboardDescriptor.getMaxAge()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
//            maxAgeText.addModifyListener(e -> {
//                dashboardDescriptor.setMaxAge(DashboardUtils.parseDuration(maxAgeText.getText(), dashboardDescriptor.getMaxAge()));
//            });
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }

    private void saveSettings() {
        dashboardDescriptor.setId(idText.getText());
        dashboardDescriptor.setName(nameText.getText());
        dashboardDescriptor.setDescription(descriptionText.getText());
        dashboardDescriptor.setDataType(DashboardDataType.values()[dataTypeCombo.getSelectionIndex()]);
        dashboardDescriptor.setCalcType(DashboardCalcType.values()[calcTypeCombo.getSelectionIndex()]);
        dashboardDescriptor.setValueType(DashboardValueType.values()[valueTypeCombo.getSelectionIndex()]);
        dashboardDescriptor.setFetchType(DashboardFetchType.values()[fetchTypeCombo.getSelectionIndex()]);
        dashboardDescriptor.setQueries(queryText.getText().split("\\n\\s*\\n"));

        dashboardDescriptor.setDefaultViewType((DashboardViewTypeDescriptor) viewTypes.get(viewTypeCombo.getSelectionIndex()));
        dashboardDescriptor.setUpdatePeriod(CommonUtils.toLong(updatePeriodText.getText(), dashboardDescriptor.getUpdatePeriod()));
        dashboardDescriptor.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), dashboardDescriptor.getMaxItems()));
        dashboardDescriptor.setMaxAge(DashboardUtils.parseDuration(maxAgeText.getText(), dashboardDescriptor.getMaxAge()));
    }

    @Override
    protected void okPressed() {
        saveSettings();
        super.okPressed();
    }

}
