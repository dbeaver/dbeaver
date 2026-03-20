/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVGroupRowStriping;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Configure alternating row backgrounds by consecutive equal values in one or more columns (#40259).
 */
public class GroupRowStripingDialog extends BaseDialog {
    private static final int DEFAULT_STRIPE_BLEND_PERCENT = 12;

    @NotNull
    private final ResultSetViewer resultSetViewer;
    @NotNull
    private final DBVEntity vEntitySrc;
    @NotNull
    private final DBVEntity vEntity;

    private Button enableCheck;
    private List columnList;
    private Combo attributeCombo;
    private Button sortByGroupCheck;
    private ColorSelector colorSelector1;
    private ColorSelector colorSelector2;
    private Button btnAdd;
    private Button btnRemove;
    private Button btnUp;
    private Button btnDown;

    public GroupRowStripingDialog(@NotNull ResultSetViewer resultSetViewer, @NotNull DBVEntity vEntity) {
        super(resultSetViewer.getControl().getShell(), ResultSetMessages.dialog_group_row_striping_title, UIIcon.PALETTE);
        this.resultSetViewer = resultSetViewer;
        this.vEntitySrc = vEntity;
        this.vEntity = new DBVEntity(vEntity.getContainer(), vEntity, vEntity.getModel());
    }

    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);
        ((GridLayout) composite.getLayout()).numColumns = 1;

        enableCheck = UIUtils.createCheckbox(composite, ResultSetMessages.dialog_group_row_striping_enable, false);
        enableCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEnableState();
            }
        });

        UIUtils.createControlLabel(composite, ResultSetMessages.dialog_group_row_striping_columns_label);
        columnList = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        GridData listGd = new GridData(GridData.FILL_BOTH);
        listGd.minimumHeight = 120;
        listGd.widthHint = 360;
        columnList.setLayoutData(listGd);

        Composite buttonRow = UIUtils.createComposite(composite, 5);
        attributeCombo = new Combo(buttonRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData comboGd = new GridData(GridData.FILL_HORIZONTAL);
        comboGd.horizontalSpan = 1;
        comboGd.widthHint = 200;
        attributeCombo.setLayoutData(comboGd);

        btnAdd = UIUtils.createPushButton(buttonRow, ResultSetMessages.dialog_group_row_striping_add, null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    addSelectedColumn();
                }
            });
        btnRemove = UIUtils.createPushButton(buttonRow, ResultSetMessages.dialog_group_row_striping_remove, null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    removeSelectedColumn();
                }
            });
        btnUp = UIUtils.createPushButton(buttonRow, ResultSetMessages.dialog_group_row_striping_up, null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveColumn(-1);
                }
            });
        btnDown = UIUtils.createPushButton(buttonRow, ResultSetMessages.dialog_group_row_striping_down, null,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveColumn(1);
                }
            });

        sortByGroupCheck = UIUtils.createCheckbox(
            composite,
            ResultSetMessages.dialog_group_row_striping_sort_by_group,
            false);
        Label sortTip = new Label(composite, SWT.WRAP);
        sortTip.setText(ResultSetMessages.dialog_group_row_striping_sort_by_group_tip);
        GridData tipGd = new GridData(GridData.FILL_HORIZONTAL);
        tipGd.widthHint = 420;
        sortTip.setLayoutData(tipGd);

        UIUtils.createControlLabel(composite, ResultSetMessages.dialog_group_row_striping_color_first);
        colorSelector1 = new ColorSelector(composite);

        UIUtils.createControlLabel(composite, ResultSetMessages.dialog_group_row_striping_color_second);
        colorSelector2 = new ColorSelector(composite);

        loadFromEntity();
        fillAttributeCombo();
        updateEnableState();
        return composite;
    }

    private void loadFromEntity() {
        columnList.removeAll();
        DBVGroupRowStriping grs = vEntity.getGroupRowStriping();
        RGB defaultRgb = resultSetViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
        RGB alternateRgb = makeAlternateStripeDefault(defaultRgb);
        if (grs == null || !grs.isEnabled() || CommonUtils.isEmpty(grs.getColumnNames())) {
            enableCheck.setSelection(false);
            sortByGroupCheck.setSelection(false);
            colorSelector1.setColorValue(defaultRgb);
            colorSelector2.setColorValue(alternateRgb);
            return;
        }
        enableCheck.setSelection(true);
        sortByGroupCheck.setSelection(grs.isSortByGroupColumns());
        for (String col : grs.getColumnNames()) {
            columnList.add(col);
        }
        colorSelector1.setColorValue(parseRgb(grs.getBackgroundColor1(), defaultRgb));
        colorSelector2.setColorValue(parseRgb(grs.getBackgroundColor2(), alternateRgb));
    }

    @NotNull
    private static RGB makeAlternateStripeDefault(@NotNull RGB base) {
        RGB blend = UIUtils.isDark(base) ? new RGB(255, 255, 255) : new RGB(0, 0, 0);
        return UIUtils.blend(base, blend, DEFAULT_STRIPE_BLEND_PERCENT);
    }

    private static RGB parseRgb(String s, RGB fallback) {
        if (CommonUtils.isEmpty(s)) {
            return fallback;
        }
        try {
            return StringConverter.asRGB(s);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private void fillAttributeCombo() {
        attributeCombo.removeAll();
        Set<String> inList = new LinkedHashSet<>();
        for (String item : columnList.getItems()) {
            inList.add(item);
        }
        for (DBDAttributeBinding attr : resultSetViewer.getModel().getVisibleAttributes()) {
            if (!inList.contains(attr.getName())) {
                attributeCombo.add(attr.getName());
            }
        }
        if (attributeCombo.getItemCount() > 0) {
            attributeCombo.select(0);
        }
    }

    private void updateEnableState() {
        boolean on = enableCheck.getSelection();
        columnList.setEnabled(on);
        attributeCombo.setEnabled(on);
        sortByGroupCheck.setEnabled(on);
        colorSelector1.setEnabled(on);
        colorSelector2.setEnabled(on);
        btnAdd.setEnabled(on);
        btnRemove.setEnabled(on);
        btnUp.setEnabled(on);
        btnDown.setEnabled(on);
    }

    private void addSelectedColumn() {
        int idx = attributeCombo.getSelectionIndex();
        if (idx < 0) {
            return;
        }
        String name = attributeCombo.getItem(idx);
        columnList.add(name);
        attributeCombo.remove(idx);
        if (attributeCombo.getItemCount() > 0) {
            attributeCombo.select(Math.min(idx, attributeCombo.getItemCount() - 1));
        }
    }

    private void removeSelectedColumn() {
        int idx = columnList.getSelectionIndex();
        if (idx < 0) {
            return;
        }
        String name = columnList.getItem(idx);
        columnList.remove(idx);
        fillAttributeCombo();
        for (int k = 0; k < attributeCombo.getItemCount(); k++) {
            if (name.equals(attributeCombo.getItem(k))) {
                attributeCombo.select(k);
                return;
            }
        }
        if (attributeCombo.getItemCount() > 0) {
            attributeCombo.select(0);
        }
    }

    private void moveColumn(int delta) {
        int i = columnList.getSelectionIndex();
        int j = i + delta;
        if (i < 0 || j < 0 || j >= columnList.getItemCount()) {
            return;
        }
        String[] items = columnList.getItems();
        String t = items[i];
        items[i] = items[j];
        items[j] = t;
        columnList.setItems(items);
        columnList.select(j);
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        if (enableCheck.getSelection() && columnList.getItemCount() == 0) {
            UIUtils.showMessageBox(
                getShell(),
                ResultSetMessages.dialog_group_row_striping_validation_title,
                ResultSetMessages.dialog_group_row_striping_validation_columns,
                SWT.ICON_WARNING);
            return;
        }
        if (enableCheck.getSelection()) {
            DBVGroupRowStriping grs = new DBVGroupRowStriping();
            grs.setEnabled(true);
            grs.setSortByGroupColumns(sortByGroupCheck.getSelection());
            grs.setColumnNames(Arrays.asList(columnList.getItems()));
            grs.setBackgroundColor1(StringConverter.asString(colorSelector1.getColorValue()));
            grs.setBackgroundColor2(StringConverter.asString(colorSelector2.getColorValue()));
            vEntity.setGroupRowStriping(grs);
        } else {
            vEntity.setGroupRowStriping(null);
        }
        vEntitySrc.copyFrom(vEntity, vEntity.getModel());
        vEntitySrc.persistConfiguration();
        resultSetViewer.getModel().updateColorMapping(vEntitySrc, true);
        resultSetViewer.redrawData(false, false);
        super.okPressed();
    }

    @Override
    public boolean close() {
        vEntity.dispose();
        return super.close();
    }
}
