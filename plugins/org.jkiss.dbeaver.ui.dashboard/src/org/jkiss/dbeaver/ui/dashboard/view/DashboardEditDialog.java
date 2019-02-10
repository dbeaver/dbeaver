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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardViewTypeDescriptor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DashboardEditDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardEditDialog";//$NON-NLS-1$

    private final DashboardDescriptor dashboardDescriptor;

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

            UIUtils.createLabelText(infoGroup, "ID", dashboardDescriptor.getId(), SWT.BORDER | baseStyle)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            UIUtils.createLabelText(infoGroup, "Name", dashboardDescriptor.getName(), SWT.BORDER | baseStyle)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
//            UIUtils.createLabelText(infoGroup, "Group", CommonUtils.notEmpty(dashboardDescriptor.getGroup()), SWT.BORDER | baseStyle)
//                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            UIUtils.createLabelText(infoGroup, "Data type", dashboardDescriptor.getDataType().name(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, "Calc type", dashboardDescriptor.getCalcType().name(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, "Value type", dashboardDescriptor.getValueType().name(), SWT.BORDER | SWT.READ_ONLY);
            UIUtils.createLabelText(infoGroup, "Fetch type", dashboardDescriptor.getFetchType().name(), SWT.BORDER | SWT.READ_ONLY);

            Text descriptionText = UIUtils.createLabelText(infoGroup, "Description", CommonUtils.notEmpty(dashboardDescriptor.getDescription()), SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | baseStyle);
            ((GridData)descriptionText.getLayoutData()).heightHint = 30;
            ((GridData)descriptionText.getLayoutData()).widthHint = 300;
            ((GridData)descriptionText.getLayoutData()).horizontalSpan = 3;
            descriptionText.addModifyListener(e -> {
                dashboardDescriptor.setDescription(descriptionText.getText());
            });
        }

        {
            Group sqlGroup = UIUtils.createControlGroup(composite, "Queries", 1, GridData.FILL_HORIZONTAL, 0);
            Text queryText = new Text(sqlGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | baseStyle);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 100;
            gd.widthHint = 300;
            queryText.setLayoutData(gd);
            UIUtils.createInfoLabel(sqlGroup, "Use blank line as query separator");

            StringBuilder sql = new StringBuilder();
            for (DashboardDescriptor.QueryMapping query : dashboardDescriptor.getQueries()) {
                sql.append(query.getQueryText()).append("\n\n");
            }
            queryText.setText(sql.toString());
        }

        {
            Group updateGroup = UIUtils.createControlGroup(composite, "Rendering", 2, GridData.FILL_HORIZONTAL, 0);

            Combo typeCombo = UIUtils.createLabelCombo(updateGroup, "Default view", "Dashboard view", SWT.BORDER | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            {
                List<DashboardViewType> viewTypes = DashboardRegistry.getInstance().getSupportedViewTypes(dashboardDescriptor.getDataType());
                for (DashboardViewType viewType : viewTypes) {
                    typeCombo.add(viewType.getTitle());
                }
                typeCombo.setText(dashboardDescriptor.getDefaultViewType().getTitle());
                typeCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        dashboardDescriptor.setDefaultViewType((DashboardViewTypeDescriptor) viewTypes.get(typeCombo.getSelectionIndex()));
                    }
                });
            }
            typeCombo.setEnabled(!readOnly);

            Text updatePeriodText = UIUtils.createLabelText(updateGroup, "Update period (ms)", String.valueOf(dashboardDescriptor.getUpdatePeriod()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
            updatePeriodText.addModifyListener(e -> {
                dashboardDescriptor.setUpdatePeriod(CommonUtils.toLong(updatePeriodText.getText(), dashboardDescriptor.getUpdatePeriod()));
            });
            Text maxItemsText = UIUtils.createLabelText(updateGroup, "Maximum items", String.valueOf(dashboardDescriptor.getMaxItems()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
            maxItemsText.addModifyListener(e -> {
                dashboardDescriptor.setMaxItems(CommonUtils.toInt(maxItemsText.getText(), dashboardDescriptor.getMaxItems()));
            });
            Text maxAgeText = UIUtils.createLabelText(updateGroup, "Maximum age (ISO-8601)", DashboardUtils.formatDuration(dashboardDescriptor.getMaxAge()), SWT.BORDER | baseStyle, new GridData(GridData.FILL_HORIZONTAL));
            maxAgeText.addModifyListener(e -> {
                dashboardDescriptor.setMaxAge(DashboardUtils.parseDuration(maxAgeText.getText(), dashboardDescriptor.getMaxAge()));
            });
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }

}
