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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
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
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.StringValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorSettingsDialog extends BaseDialog {

    private static final Log log = Log.getLog(ColorSettingsDialog.class);

    private static final String DIALOG_ID = "DBeaver.ColorSettingsDialog2";//$NON-NLS-1$

    /** List of supported <b>binary</b> operators the user can pick from. */
    private static final DBCLogicalOperator[] SUPPORTED_OPERATORS = {
        DBCLogicalOperator.EQUALS,
        DBCLogicalOperator.NOT_EQUALS,
        DBCLogicalOperator.GREATER,
        DBCLogicalOperator.GREATER_EQUALS,
        DBCLogicalOperator.LESS,
        DBCLogicalOperator.LESS_EQUALS,
        DBCLogicalOperator.ILIKE,
        DBCLogicalOperator.LIKE,
        DBCLogicalOperator.NOT_LIKE,
        DBCLogicalOperator.REGEX
    };

    /** List of operators from {@code SUPPORTED_OPERATORS} that operate on strings only. */
    private static final DBCLogicalOperator[] STRING_OPERATORS = {
        DBCLogicalOperator.ILIKE,
        DBCLogicalOperator.LIKE,
        DBCLogicalOperator.NOT_LIKE,
        DBCLogicalOperator.REGEX
    };

    private static RGB DEFAULT_RGB;

    @NotNull
    private final ResultSetViewer resultSetViewer;
    @Nullable
    private DBDAttributeBinding attribute;
    @Nullable
    private ResultSetRow row;

    private final DBVEntity vEntitySrc;
    private final DBVEntity vEntity;

    private Table colorsTable;
    private Combo operatorCombo;
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
    private ToolItem btnDelete;
    private Table attributeTable;

    private Composite editorPlaceholder1;
    private Composite editorPlaceholder2;

    public ColorSettingsDialog(
        @NotNull ResultSetViewer resultSetViewer,
        @NotNull DBVEntity vEntity,
        @NotNull final DBDAttributeBinding attr,
        @Nullable final ResultSetRow row)
    {
        super(resultSetViewer.getControl().getShell(), NLS.bind(ResultSetMessages.dialog_row_colors_title, attr.getDataContainer().getName()), UIIcon.PALETTE);
        this.resultSetViewer = resultSetViewer;
        this.attribute = attr;
        this.row = row;

        this.vEntitySrc = vEntity;
        this.vEntity = new DBVEntity(vEntitySrc.getContainer(), vEntitySrc, vEntitySrc.getModel());

        DEFAULT_RGB = resultSetViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
    }

/*
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }
*/

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        SashForm divider = new SashForm(composite, SWT.HORIZONTAL);
        divider.setSashWidth(10);
        divider.setLayoutData(new GridData(GridData.FILL_BOTH));
        createAttributeSelectorArea(divider);
        createAttributeSettingsArea(divider);

        updateAttributeSelection();

        return parent;
    }

    private void createAttributeSelectorArea(Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);

        attributeTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
        attributeTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        attributeTable.setLayoutData(gd);
        UIUtils.executeOnResize(attributeTable, () -> UIUtils.packColumns(attributeTable, true));

        UIUtils.createTableColumn(attributeTable, SWT.LEFT, ResultSetMessages.dialog_row_colors_table_attributes_name);
        UIUtils.createTableColumn(attributeTable, SWT.LEFT, ResultSetMessages.dialog_row_colors_table_attributes_color);

        for (DBDAttributeBinding attr : resultSetViewer.getModel().getVisibleAttributes()) {
            TableItem attrItem = new TableItem(attributeTable, SWT.NONE);
            attrItem.setData(attr);
            attrItem.setText(0, attr.getName());
            attrItem.setImage(0, DBeaverIcons.getImage(DBValueFormatting.getObjectImage(attr, true)));

            if (this.attribute == attr) {
                attributeTable.setSelection(attrItem);
            }
            //updateColumnItem(attrItem);
        }

        attributeTable.addListener(SWT.PaintItem, event -> {
            if (event.index == 1) {

                int x = event.x + 4;
                DBDAttributeBinding attr = (DBDAttributeBinding) event.item.getData();
                List<DBVColorOverride> coList = vEntity.getColorOverrides(attr.getName());
                if (!coList.isEmpty()) {
                    for (DBVColorOverride co : coList) {
                        List<String> coStrings = new ArrayList<>();
                        if (co.getAttributeValues() != null) {
                            for (Object value : co.getAttributeValues()) {
                                coStrings.add(CommonUtils.toString(value));
                            }
                        }
                        //String colorSettings = "   ";//String.join(", ", coStrings);
                        //Point textSize = event.gc.stringExtent(colorSettings);
                        int boxSize = attributeTable.getItemHeight() - 4;
                        Point textSize = new Point(boxSize, boxSize);
                        Color fg = attributeTable.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);//getColorTableForeground(co);
                        Color bg = getColorTableBackground(co);
                        if (fg != null) event.gc.setForeground(fg);
                        if (bg != null) event.gc.setBackground(bg);

                        event.gc.fillRectangle(x, event.y + 2, textSize.x, textSize.y);
                        event.gc.drawRectangle(x, event.y + 2, textSize.x - 1, textSize.y - 1);
                        //event.gc.drawText(colorSettings, x, event.y);
                        x += textSize.x + 4;
                    }
                }
            }
        });
        attributeTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateAttributeSelection();
            }
        });
    }

    private void updateAttributeSelection() {
        TableItem[] selection = attributeTable.getSelection();
        if (selection.length == 0) {
            attribute = null;
        } else {
            attribute = (DBDAttributeBinding) selection[0].getData();
        }

        colorsTable.removeAll();
        if (attribute == null) {
            // Nothing to load
            curOverride = null;
            colorOverrides = null;
        } else {
            colorOverrides = vEntity.getColorOverrides(attribute.getName());
            if (colorOverrides == null) {
                colorOverrides = new ArrayList<>();
            }

            for (DBVColorOverride co : colorOverrides) {
                TableItem tableItem = new TableItem(colorsTable, SWT.NONE);
                tableItem.setData(co);
                updateColorItem(tableItem);
            }
            if (!colorOverrides.isEmpty()) {
                curOverride = colorOverrides.get(0);
                colorsTable.setSelection(0);
            } else {
                curOverride = null;
            }
        }

        updateControlsState();
    }

    private void createAttributeSettingsArea(Composite composite) {
        Composite mainGroup = UIUtils.createComposite(composite, 1);
        mainGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));

        {
            Composite colorsGroup = UIUtils.createComposite(mainGroup, 2);
            colorsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));

            //UIUtils.createControlLabel(colorsGroup, "Cell conditions");

            colorsTable = new Table(colorsGroup, SWT.BORDER | SWT.FULL_SELECTION);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.heightHint = 100;
            colorsTable.setLayoutData(gd);

            colorsTable.addListener(SWT.EraseItem, event -> {
                if ((event.detail & SWT.SELECTED) != 0) {
                    //event.detail &= ~SWT.SELECTED;
                    Color bgColor = getColorTableBackground((DBVColorOverride) event.item.getData());
                    if (bgColor != null) {
                        event.gc.setBackground(bgColor);
                    } else {
                        event.gc.setBackground(colorsTable.getBackground());
                    }
                    event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                    event.gc.drawRectangle(event.x, event.y, event.width - 1, event.height - 1);
                }
            });
/*
            colorsTable.addListener(SWT.PaintItem, event -> {
                if ((event.detail & SWT.SELECTED) == 0) {
                    return;
                }
                event.gc.drawText(((TableItem) event.item).getText(), event.x, event.y);
            });
*/

            //UIUtils.createTableColumn(colorsTable, SWT.LEFT, "Operator");
            UIUtils.createTableColumn(colorsTable, SWT.RIGHT, "Value(s)");
            UIUtils.executeOnResize(colorsTable, () -> UIUtils.packColumns(colorsTable, true));

            colorsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = colorsTable.getSelectionIndex();
                    curOverride = selectionIndex < 0 ? null : (DBVColorOverride) colorsTable.getItem(selectionIndex).getData();
                    btnDelete.setEnabled(selectionIndex >= 0);
                    updateControlsState();
                }
            });

            {
                ToolBar buttonsPanel = new ToolBar(colorsGroup, SWT.FLAT | SWT.VERTICAL);
                buttonsPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
                UIUtils.createToolItem(buttonsPanel, "Add", UIIcon.ROW_ADD, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        curOverride = new DBVColorOverride(attribute.getName(), DBCLogicalOperator.EQUALS, null, null, null);
                        vEntity.addColorOverride(curOverride);
                        TableItem tableItem = new TableItem(colorsTable, SWT.NONE);
                        tableItem.setData(curOverride);
                        colorsTable.setSelection(tableItem);
                        updateColorItem(tableItem);
                        updateControlsState();
                    }
                });
                btnDelete = UIUtils.createToolItem(buttonsPanel, "Delete", UIIcon.ROW_DELETE, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (curOverride != null) {
                            colorsTable.getItem(colorsTable.getSelectionIndex()).dispose();
                            vEntity.removeColorOverride(curOverride);
                            curOverride = null;
                            updateControlsState();
                        }
                    }
                });
                btnDelete.setEnabled(false);
            }

        }

        {
            settingsGroup = new Composite(mainGroup, SWT.NONE);
            settingsGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
            settingsGroup.setLayout(new GridLayout(3, false));
            GridData gd = new GridData();
            gd.horizontalSpan = 3;
            UIUtils.createControlLabel(settingsGroup, ResultSetMessages.dialog_row_colors_group_settings_label).setLayoutData(gd);

            rangeCheck = UIUtils.createCheckbox(
                settingsGroup,
                ResultSetMessages.dialog_row_colors_group_settings_range_label,
                ResultSetMessages.dialog_row_colors_group_settings_range_tip,
                false,
                3
            );
            rangeCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curOverride != null) {
                        curOverride.setRange(rangeCheck.getSelection());
                    }
                    updateControlsState();
                }
            });
            singleColumnCheck = UIUtils.createCheckbox(
                settingsGroup,
                ResultSetMessages.dialog_row_colors_group_settings_single_column_label,
                ResultSetMessages.dialog_row_colors_group_settings_single_column_tip,
                false,
                3
            );
            singleColumnCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curOverride != null) {
                        curOverride.setSingleColumn(singleColumnCheck.getSelection());
                    }
                }
            });

            operatorCombo = UIUtils.createLabelCombo(
                settingsGroup,
                ResultSetMessages.dialog_row_colors_group_settings_operator_label,
                ResultSetMessages.dialog_row_colors_group_settings_operator_tip,
                SWT.DROP_DOWN | SWT.READ_ONLY
            );
            operatorCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (curOverride != null) {
                        curOverride.setOperator(SUPPORTED_OPERATORS[operatorCombo.getSelectionIndex()]);
                    }
                    updateControlsState();
                }
            });
            UIUtils.createPlaceholder(settingsGroup, 1);

            for (DBCLogicalOperator operator : SUPPORTED_OPERATORS) {
                operatorCombo.add(operator.getExpression());
            }

            UIUtils.createControlLabel(settingsGroup, ResultSetMessages.dialog_row_colors_group_settings_value_label);

            editorPlaceholder1 = new Composite(settingsGroup, SWT.NONE);
            editorPlaceholder1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            editorPlaceholder1.setLayout(new FillLayout());
            editorPlaceholder2 = new Composite(settingsGroup, SWT.NONE);
            editorPlaceholder2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            editorPlaceholder2.setLayout(new FillLayout());

            valueEditor1 = createValueEditor(editorPlaceholder1, 0);
            valueEditor2 = createValueEditor(editorPlaceholder2, 1);

            UIUtils.createControlLabel(settingsGroup, ResultSetMessages.dialog_row_colors_group_settings_background_color_label);
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
            UIUtils.createControlLabel(settingsGroup, ResultSetMessages.dialog_row_colors_group_settings_foreground_color_label);
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
                ResultSetMessages.dialog_row_colors_group_settings_tip,
                GridData.FILL_HORIZONTAL, 3);
        }
    }

    private void updateSettingsValues() {
        try {
            if (curOverride == null) {
                UIUtils.disposeChildControls(editorPlaceholder1);
                new Text(editorPlaceholder1, SWT.BORDER);
                UIUtils.disposeChildControls(editorPlaceholder2);
                new Text(editorPlaceholder2, SWT.BORDER);
                valueEditor1 = null;
                valueEditor2 = null;
                operatorCombo.select(0);
                rangeCheck.setSelection(false);
                singleColumnCheck.setSelection(false);
                bgColorSelector1.setColorValue(DEFAULT_RGB);
                fgColorSelector1.setColorValue(DEFAULT_RGB);
                bgColorSelector2.setColorValue(DEFAULT_RGB);
                fgColorSelector2.setColorValue(DEFAULT_RGB);
            } else {
                operatorCombo.select(Math.max(ArrayUtils.indexOf(SUPPORTED_OPERATORS, curOverride.getOperator()), 0));
                rangeCheck.setSelection(curOverride.isRange());
                singleColumnCheck.setSelection(curOverride.isSingleColumn());

                valueEditor1 = createValueEditor(editorPlaceholder1, 0);
                valueEditor2 = createValueEditor(editorPlaceholder2, 1);

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
            settingsGroup.layout(true, true);
            updateCurrentTreeItem();
        } catch (DBException e) {
            log.error(e);
        }
    }

    private void updateControlsState() {
        if (curOverride == null) {
            if (settingsEnableState == null) {
                settingsEnableState = ControlEnableState.disable(settingsGroup);
            }
        } else if (settingsEnableState != null) {
            settingsEnableState.restore();
            settingsEnableState = null;
        }

        updateSettingsValues();

        boolean isRange = rangeCheck.getSelection();
        if (valueEditor2 != null) {
            valueEditor2.getControl().setEnabled(isRange);
        }
        bgColorSelector2.setEnabled(isRange);
        fgColorSelector2.setEnabled(isRange);
        operatorCombo.setEnabled(!isRange);

        colorsTable.setEnabled(attribute != null);
        btnDelete.setEnabled(colorsTable.getSelectionIndex() >= 0);

    }

    private IValueEditor createValueEditor(Composite editorPlaceholder, int index) {
        try {
            UIUtils.disposeChildControls(editorPlaceholder);

            final IValueManager valueManager;
            if (curOverride == null || !ArrayUtils.contains(STRING_OPERATORS, curOverride.getOperator())) {
                valueManager = ValueManagerRegistry.findValueManager(
                    resultSetViewer.getDataSource(),
                    attribute,
                    attribute.getValueHandler().getValueObjectType(attribute));
            } else {
                valueManager = new StringValueManager();
            }
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
                            } else if (attributeValues.length != valueCount) {
                                attributeValues = Arrays.copyOf(attributeValues, valueCount);
                            }
                            attributeValues[index] = value;
                            curOverride.setAttributeValues(attributeValues);
                            updateCurrentTreeItem();
                            //updateColumnItem(attributeTable.getSelection()[0]);
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
        int itemIndex = colorsTable.getSelectionIndex();
        if (itemIndex >= 0) {
            updateColorItem(colorsTable.getItem(itemIndex));
        }
        attributeTable.redraw();
        //colorsTable.getColumn(0).setWidth(colorsTable.getSize().x);
    }

    private void updateColorItem(TableItem tableItem) {
        DBVColorOverride co = (DBVColorOverride) tableItem.getData();
        String text;
        Object[] values = co.getAttributeValues();
        if (ArrayUtils.isEmpty(values)) {
            text = co.getOperator().getExpression() + " ?";
        } else if (values.length == 1) {
            text = co.getOperator().getExpression() + " " + DBValueFormatting.getDefaultValueDisplayString(values[0], DBDDisplayFormat.UI);
        } else {
            if (co.isRange()) {
                text = "In " + Arrays.toString(values);
            } else {
                text = co.getOperator().getExpression() + " " + Arrays.toString(values);
            }
        }
        tableItem.setText(0, text);
        tableItem.setForeground(getColorTableForeground((DBVColorOverride) tableItem.getData()));
        tableItem.setBackground(getColorTableBackground((DBVColorOverride) tableItem.getData()));
    }

    private Color getColorTableForeground(DBVColorOverride co) {
        if (!CommonUtils.isEmpty(co.getColorForeground())) {
            return UIUtils.getSharedColor(co.getColorForeground());
        }
        return null;
    }

    private Color getColorTableBackground(DBVColorOverride co) {
        if (!co.isRange()) {
            if (!CommonUtils.isEmpty(co.getColorBackground())) {
                return UIUtils.getSharedColor(co.getColorBackground());
            }
        } else {
            String bg1 = co.getColorBackground();
            String bg2 = co.getColorBackground2();
            if (!CommonUtils.isEmpty(bg1) && !CommonUtils.isEmpty(bg2)) {
                Color bg1Color = UIUtils.getSharedColor(bg1);
                Color bg2Color = UIUtils.getSharedColor(bg2);
                return UIUtils.getSharedColor(ResultSetUtils.makeGradientValue(bg1Color.getRGB(), bg2Color.getRGB(), 1, 100, 50));
            }
        }
        return null;
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
        vEntitySrc.copyFrom(vEntity, vEntity.getModel());
        vEntitySrc.persistConfiguration();
        resultSetViewer.getModel().updateColorMapping(vEntitySrc, true);

        super.okPressed();
    }

    @Override
    public boolean close() {
        if (this.vEntity != null) {
            this.vEntity.dispose();
        }
        return super.close();
    }

    private class ColorValueController extends ResultSetValueController {

        ColorValueController(@Nullable Composite inlinePlaceholder) {
            super(resultSetViewer,
                new ResultSetCellLocation(attribute, row),
                EditType.INLINE,
                inlinePlaceholder);
        }

        void setInlinePlaceholder(Composite ph) {
            inlinePlaceholder = ph;
        }

        @Override
        public boolean isReadOnly() {
            // Color range values are always editable
            return false;
        }

        @Override
        public void updateValue(@Nullable Object value, boolean updatePresentation) {
            // Do not update
        }
    }

}
