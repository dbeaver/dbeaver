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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ColorSettingsDialog extends BaseDialog {

    private static final Log log = Log.getLog(ColorSettingsDialog.class);
    private static final String DIALOG_ID = "DBeaver.ColorSettingsDialog";//$NON-NLS-1$
    private static RGB DEFAULT_RGB;

    @NotNull
    private final ResultSetViewer resultSetViewer;
    @NotNull
    private final DBDAttributeBinding attribute;
    @Nullable
    private final ResultSetRow row;

    private Table colorsTable;
    private Button rangeCheck;
    private ColorSelector bgColorSelector1;
    private ColorSelector bgColorSelector2;
    private ColorSelector fgColorSelector1;
    private ColorSelector fgColorSelector2;
    private IValueEditor valueEditor1;
    private IValueEditor valueEditor2;

    private ControlEnableState settingsEnableState;
    private Composite settingsGroup;
    private List<DBVColorOverride> colorOverrides;
    private Button singleColumnCheck;

    private DBVColorOverride curOverride;

    public ColorSettingsDialog(
        @NotNull ResultSetViewer resultSetViewer,
        @NotNull final DBDAttributeBinding attr,
        @Nullable final ResultSetRow row) {
        super(resultSetViewer.getControl().getShell(), "Customize colors for [" + attr.getName() + "]", UIIcon.PALETTE);
        this.resultSetViewer = resultSetViewer;
        this.attribute = attr;
        this.row = row;

        DEFAULT_RGB = resultSetViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        Composite mainGroup = new Composite(composite, SWT.NONE);
        mainGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
        mainGroup.setLayout(new GridLayout(2, false));

        GridData gd;
/*
        gd.horizontalSpan = 2;
        Composite titlePanel = UIUtils.createComposite(mainGroup, 2);
        titlePanel.setLayoutData(gd);
        UIUtils.createLabelText(titlePanel, "Attribute", attribute.getName(), SWT.BORDER | SWT.READ_ONLY);
*/

        {
            Composite colorsGroup = new Composite(mainGroup, SWT.NONE);
            colorsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
            colorsGroup.setLayout(new GridLayout(1, false));

            UIUtils.createControlLabel(colorsGroup, "Cell conditions");

            colorsTable = new Table(colorsGroup, SWT.BORDER | SWT.FULL_SELECTION);
            //colorsTable.setHeaderVisible(true);
            gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            colorsTable.setLayoutData(gd);
            UIUtils.createTableColumn(colorsTable, SWT.LEFT, "Operator");
            UIUtils.createTableColumn(colorsTable, SWT.RIGHT, "Value(s)");
            UIUtils.executeOnResize(colorsTable, () -> UIUtils.packColumns(colorsTable, true));

            DBVEntity vEntity = DBVUtils.getVirtualEntity(attribute, true);
            //DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attribute, true);
            colorOverrides = vEntity.getColorOverrides(attribute.getName());
            if (colorOverrides == null) {
                colorOverrides = new ArrayList<>();
            }
            for (DBVColorOverride co : colorOverrides) {
                TableItem tableItem = new TableItem(colorsTable, SWT.NONE);
                tableItem.setData(co);
                updateTreeItem(tableItem);
            }

            //ToolBar toolbar = new ToolBar(colorsGroup, SWT.FLAT | SWT.HORIZONTAL);
            Composite buttonsPanel = UIUtils.createComposite(colorsGroup, 2);
            Button btnAdd = createButton(buttonsPanel, 1000, "Add", false);
            btnAdd.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    curOverride = new DBVColorOverride(attribute.getName(), DBCLogicalOperator.EQUALS, null, null, null);
                    vEntity.addColorOverride(curOverride);
                    TableItem tableItem = new TableItem(colorsTable, SWT.NONE);
                    tableItem.setData(curOverride);
                    colorsTable.setSelection(tableItem);
                    updateTreeItem(tableItem);
                    updateSettingsValues();
                    updateControlsState();
                }
            });
            Button btnDelete = createButton(buttonsPanel, 1001, "Delete", false);
            btnDelete.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curOverride != null) {
                        colorsTable.getItem(colorsTable.getSelectionIndex()).dispose();
                        vEntity.removeColorOverride(curOverride);
                        curOverride = null;
                        updateSettingsValues();
                        updateControlsState();
                    }
                }
            });

            colorsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = colorsTable.getSelectionIndex();
                    curOverride = selectionIndex < 0 ? null : (DBVColorOverride) colorsTable.getItem(selectionIndex).getData();
                    btnDelete.setEnabled(selectionIndex >= 0);
                    updateSettingsValues();
                    updateControlsState();
                }
            });
        }

        {
            settingsGroup = new Composite(mainGroup, SWT.NONE);
            settingsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
            settingsGroup.setLayout(new GridLayout(3, false));
            gd = new GridData();
            gd.horizontalSpan = 3;
            UIUtils.createControlLabel(settingsGroup, "Settings").setLayoutData(gd);

            rangeCheck = UIUtils.createCheckbox(settingsGroup, "Range / gradient", "Use value range / color gradient", false, 3);
            rangeCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateControlsState();
                    if (curOverride != null) {
                        curOverride.setRange(rangeCheck.getSelection());
                    }
                }
            });
            singleColumnCheck = UIUtils.createCheckbox(settingsGroup, "Apply colors to this column only", "Apply colors to this column only, otherwise color full row", false, 3);
            singleColumnCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curOverride != null) {
                        curOverride.setSingleColumn(singleColumnCheck.getSelection());
                    }
                }
            });

            UIUtils.createControlLabel(settingsGroup, "Value(s)");
            valueEditor1 = createValueEditor(settingsGroup, 0);
            valueEditor2 = createValueEditor(settingsGroup, 1);

            UIUtils.createControlLabel(settingsGroup, "Background");
            bgColorSelector1 = new ColorSelector(settingsGroup);
            bgColorSelector1.addListener(event -> {
                curOverride.setColorBackground(StringConverter.asString(bgColorSelector1.getColorValue()));
                updateCurrentTreeItem();
            });
            bgColorSelector2 = new ColorSelector(settingsGroup);
            bgColorSelector2.addListener(event -> {
                curOverride.setColorBackground2(StringConverter.asString(bgColorSelector2.getColorValue()));
                updateCurrentTreeItem();
            });
            UIUtils.createControlLabel(settingsGroup, "Foreground");
            fgColorSelector1 = new ColorSelector(settingsGroup);
            fgColorSelector1.addListener(event -> {
                curOverride.setColorForeground(StringConverter.asString(fgColorSelector1.getColorValue()));
                updateCurrentTreeItem();
            });
            fgColorSelector2 = new ColorSelector(settingsGroup);
            fgColorSelector2.addListener(event -> {
                curOverride.setColorForeground2(StringConverter.asString(fgColorSelector2.getColorValue()));
                updateCurrentTreeItem();
            });

            UIUtils.createInfoLabel(settingsGroup,
                "To use gradient set minimum and maximum\ncolumn values and two\ncolors for gradient range. ",
                GridData.FILL_HORIZONTAL, 3);
        }

        updateControlsState();

        return parent;
    }

    private void updateSettingsValues() {
        try {
            if (curOverride == null) {
                if (valueEditor1 != null) valueEditor1.primeEditorValue(null);
                if (valueEditor2 != null) valueEditor2.primeEditorValue(null);
                rangeCheck.setSelection(false);
                singleColumnCheck.setSelection(false);
                bgColorSelector1.setColorValue(DEFAULT_RGB);
                fgColorSelector1.setColorValue(DEFAULT_RGB);
                bgColorSelector2.setColorValue(DEFAULT_RGB);
                fgColorSelector2.setColorValue(DEFAULT_RGB);
            } else {
                rangeCheck.setSelection(curOverride.isRange());
                singleColumnCheck.setSelection(curOverride.isSingleColumn());
                Object[] values = curOverride.getAttributeValues();
                if (valueEditor1 != null) {
                    valueEditor1.primeEditorValue(ArrayUtils.isEmpty(values) ? null : values[0]);
                }
                if (!CommonUtils.isEmpty(curOverride.getColorBackground())) {
                    bgColorSelector1.setColorValue(StringConverter.asRGB(curOverride.getColorBackground()));
                } else {
                    bgColorSelector1.setColorValue(DEFAULT_RGB);
                }
                if (!CommonUtils.isEmpty(curOverride.getColorForeground())) {
                    fgColorSelector1.setColorValue(StringConverter.asRGB(curOverride.getColorForeground()));
                } else {
                    fgColorSelector1.setColorValue(DEFAULT_RGB);
                }
                if (curOverride.isRange()) {
                    if (valueEditor2 != null) {
                        valueEditor2.primeEditorValue(ArrayUtils.isEmpty(values) || values.length < 2 ? null : values[1]);
                    }
                    if (!CommonUtils.isEmpty(curOverride.getColorBackground2())) {
                        bgColorSelector2.setColorValue(StringConverter.asRGB(curOverride.getColorBackground2()));
                    } else {
                        bgColorSelector2.setColorValue(DEFAULT_RGB);
                    }
                    if (!CommonUtils.isEmpty(curOverride.getColorForeground2())) {
                        fgColorSelector2.setColorValue(StringConverter.asRGB(curOverride.getColorForeground2()));
                    } else {
                        fgColorSelector2.setColorValue(DEFAULT_RGB);
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    private void updateControlsState() {
        if (curOverride == null) {
            settingsEnableState = ControlEnableState.disable(settingsGroup);
        } else if (settingsEnableState != null) {
            settingsEnableState.restore();
            settingsEnableState = null;
        }

        boolean isRange = rangeCheck.getSelection();
        if (valueEditor2 != null) {
            valueEditor2.getControl().setEnabled(isRange);
        }
        bgColorSelector2.setEnabled(isRange);
        fgColorSelector2.setEnabled(isRange);
    }

    private IValueEditor createValueEditor(Composite panel, int index) {
        try {
            IValueManager valueManager = ValueManagerRegistry.findValueManager(
                resultSetViewer.getDataContainer().getDataSource(),
                attribute,
                attribute.getValueHandler().getValueObjectType(attribute));
            ColorValueController valueController = new ColorValueController(settingsGroup) {
                @Override
                public Object getValue() {
                    if (curOverride == null ){
                        return null;
                    }
                    Object[] attributeValues = curOverride.getAttributeValues();
                    if (attributeValues == null || index > attributeValues.length - 1) {
                        return null;
                    }
                    return attributeValues[index];
                }
            };

            Composite editorPlaceholder = new Composite(panel, SWT.NONE);
            editorPlaceholder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            editorPlaceholder.setLayout(new FillLayout());
            valueController.setInlinePlaceholder(editorPlaceholder);

            IValueEditor editor = valueManager.createEditor(valueController);
            if (editor == null) {
                Label errorLabel = new Label(editorPlaceholder, SWT.NONE);
                errorLabel.setText("N/A");
            } else {
                editor.createControl();
//                if (attribute.getValueHandler() instanceof DBDValueDefaultGenerator) {
//                    Object defaultValue = ((DBDValueDefaultGenerator) attribute.getValueHandler()).generateDefaultValue(attribute);
//                    editor.primeEditorValue(defaultValue);
//                }
                editor.getControl().addListener(SWT.Modify, event -> {
                    if (curOverride != null) {
                        try {
                            Object value = editor.extractEditorValue();
                            Object[] attributeValues = curOverride.getAttributeValues();
                            int valueCount = index + 1;
                            if (attributeValues == null) {
                                attributeValues = new Object[valueCount];
                            } else if (attributeValues.length < valueCount) {
                                Object[] newAttributeValues = new Object[valueCount];
                                System.arraycopy(attributeValues, 0, newAttributeValues, 0, attributeValues.length);
                                attributeValues = newAttributeValues;
                            }
                            attributeValues[index] = value;
                            curOverride.setAttributeValues(attributeValues);
                            updateCurrentTreeItem();
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                });
            }
            return editor;
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    private void updateCurrentTreeItem() {
        updateTreeItem(colorsTable.getItem(colorsTable.getSelectionIndex()));
    }

    private void updateTreeItem(TableItem tableItem) {
        DBVColorOverride co = (DBVColorOverride) tableItem.getData();
        tableItem.setText(0, co.getOperator().getStringValue());
        Object[] values = co.getAttributeValues();
        if (ArrayUtils.isEmpty(values)) {
            tableItem.setText(1, "");
        } else if (values.length == 1) {
            tableItem.setText(1, DBValueFormatting.getDefaultValueDisplayString(values[0], DBDDisplayFormat.UI));
        } else {
            tableItem.setText(1, Arrays.toString(values));
        }
        if (!CommonUtils.isEmpty(co.getColorBackground())) {
            tableItem.setBackground(UIUtils.getSharedColor(co.getColorBackground()));
        }
        if (!CommonUtils.isEmpty(co.getColorForeground())) {
            tableItem.setForeground(UIUtils.getSharedColor(co.getColorForeground()));
        }
    }

    @Override
    public int open() {
        return super.open();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        //createButton(parent, IDialogConstants.ABORT_ID, ResultSetMessages.controls_resultset_filter_button_reset, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
    }

    @Override
    protected void okPressed() {
        resultSetViewer.getModel().updateColorMapping(true);
        super.okPressed();
    }

    private class ColorValueController extends ResultSetValueController {

        public ColorValueController(@Nullable Composite inlinePlaceholder) {
            super(resultSetViewer, attribute, null, EditType.INLINE, inlinePlaceholder);
        }

        void setInlinePlaceholder(Composite ph) {
            inlinePlaceholder = ph;
        }
    }

}
