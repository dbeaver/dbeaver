/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIElementAlignment;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DefaultColorSelector;
import org.jkiss.dbeaver.ui.controls.TextWithDropDown;
import org.jkiss.dbeaver.ui.controls.bool.BooleanMode;
import org.jkiss.dbeaver.ui.controls.bool.BooleanState;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyle;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleSet;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PrefPageDatabaseEditors
 */
public class PrefPageDatabaseEditors extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.editors"; //$NON-NLS-1$

    private Button keepEditorsOnRestart;
    private Button refreshEditorOnOpen;
    private Button editorFullName;
    private Button showTableGrid;
    private Button showPreviewOnSave;
    private Button syncEditorDataSourceWithNavigator;

    private final List<Consumer<BooleanStyleSet>> booleanStylesChangeListeners = new ArrayList<>();
    private BooleanPanel booleanCheckedPanel;
    private BooleanPanel booleanUncheckedPanel;
    private BooleanPanel booleanNullPanel;
    private RGB defaultColor;

    public PrefPageDatabaseEditors()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_editors, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            keepEditorsOnRestart = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_keep_database_editors, false);
            keepEditorsOnRestart.setToolTipText(CoreMessages.pref_page_ui_general_keep_database_editors_tip);

            refreshEditorOnOpen = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_refresh_editor_on_open, false);
            refreshEditorOnOpen.setToolTipText(CoreMessages.pref_page_ui_general_refresh_editor_on_open_tip);

            editorFullName = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_show_full_name_in_editor, false);
            showTableGrid = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_show_table_grid, false);
            showPreviewOnSave = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_show_preview_on_save, false);

            syncEditorDataSourceWithNavigator = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator, CoreMessages.pref_page_database_general_label_sync_editor_connection_with_navigator_tip, false, 2);
        }

        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_boolean, 3, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            UIUtils.createControlLabel(groupEditors, CoreMessages.pref_page_ui_general_boolean_label_mode);

            final SelectionAdapter selectionListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
                    final BooleanMode mode = (BooleanMode) e.widget.getData();
                    notifyBooleanStylesChanged(BooleanStyleSet.getDefaultStyles(store, mode));
                }
            };

            booleanStylesChangeListeners.add(value -> {
                booleanCheckedPanel.loadStyle(value.getCheckedStyle(), value.getDefaultColor());
                booleanUncheckedPanel.loadStyle(value.getUncheckedStyle(), value.getDefaultColor());
                booleanNullPanel.loadStyle(value.getNullStyle(), value.getDefaultColor());
                defaultColor = value.getDefaultColor();
            });

            for (BooleanMode mode : BooleanMode.values()) {
                final Button button = UIUtils.createRadioButton(groupEditors, mode.getName(), mode, null);
                button.setToolTipText(mode.getDescription());
                button.addSelectionListener(selectionListener);
                button.setData(mode);
                booleanStylesChangeListeners.add(value -> button.setSelection(button.getData() == value.getMode()));
            }

            final Composite group = new Composite(groupEditors, SWT.BORDER);
            group.setLayout(GridLayoutFactory.swtDefaults().numColumns(7).create());
            group.setLayoutData(GridDataFactory.swtDefaults().span(3, 1).create());

            UIUtils.createLabel(group, CoreMessages.pref_page_ui_general_boolean_label_state);
            UIUtils.createPlaceholder(group, 1);
            UIUtils.createControlLabel(group, CoreMessages.pref_page_ui_general_boolean_label_text);
            UIUtils.createPlaceholder(group, 1);
            UIUtils.createControlLabel(group, CoreMessages.pref_page_ui_general_boolean_label_align);
            UIUtils.createPlaceholder(group, 1);
            UIUtils.createControlLabel(group, CoreMessages.pref_page_ui_general_boolean_label_color);

            booleanCheckedPanel = new BooleanPanel(group, BooleanState.CHECKED);
            booleanUncheckedPanel = new BooleanPanel(group, BooleanState.UNCHECKED);
            booleanNullPanel = new BooleanPanel(group, BooleanState.NULL);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        keepEditorsOnRestart.setSelection(store.getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS));
        refreshEditorOnOpen.setSelection(store.getBoolean(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN));
        editorFullName.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME));
        showTableGrid.setSelection(store.getBoolean(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID));
        showPreviewOnSave.setSelection(store.getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW));
        syncEditorDataSourceWithNavigator.setSelection(store.getBoolean(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));

        notifyBooleanStylesChanged(BooleanStyleSet.getDefaultStyles(store));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

       store.setValue(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, keepEditorsOnRestart.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, refreshEditorOnOpen.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, editorFullName.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_EDITOR_SHOW_TABLE_GRID, showTableGrid.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW, showPreviewOnSave.getSelection());
        store.setValue(NavigatorPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, syncEditorDataSourceWithNavigator.getSelection());

        BooleanStyleSet.setDefaultStyles(store, new BooleanStyleSet(
            booleanCheckedPanel.saveStyle(),
            booleanUncheckedPanel.saveStyle(),
            booleanNullPanel.saveStyle(),
            defaultColor
        ));

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Nullable
    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }

    private void notifyBooleanStylesChanged(@NotNull BooleanStyleSet set) {
        for (Consumer<BooleanStyleSet> listener : booleanStylesChangeListeners) {
            listener.accept(set);
        }
    }

    private void updateBooleanValidState() {
        final boolean valid = booleanCheckedPanel.isValid() && booleanUncheckedPanel.isValid() && booleanNullPanel.isValid();

        if (valid) {
            setErrorMessage(null);
            setValid(true);
        } else {
            setErrorMessage(CoreMessages.pref_page_ui_general_boolean_invalid_values);
            setValid(false);
        }
    }

    private class BooleanPanel extends EventManager {
        private static final String PROP_MODE = "modeValue";
        private static final String PROP_TEXT = "textValue";
        private static final String PROP_ALIGN = "alignValue";
        private static final String PROP_COLOR = "colorValue";
        private static final String PROP_DEFAULT_COLOR = "defaultColorValue";

        private final Composite parent;
        private final BooleanState state;

        private BooleanMode currentMode;
        private String currentText;
        private UIElementAlignment currentAlignment;
        private RGB currentColor;

        public BooleanPanel(@NotNull Composite parent, @NotNull BooleanState state) {
            this.parent = parent;
            this.state = state;

            {
                final Label icon = UIUtils.createLabel(parent, state.getIcon());
                icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
                icon.setToolTipText(state.getLabel());
            }

            UIUtils.createLabel(parent, UIIcon.SEPARATOR_V);

            {
                final ModifyListener textModifyListener = e -> {
                    // Do this way to avoid recursively firing the same event over and over again.
                    // Modification to current text widget is already fired, so we're fine
                    this.currentText = ((Text) e.widget).getText();
                    updateBooleanValidState();
                };

                final SelectionListener menuSelectionListener = new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        notifyPropertyChanged(e.widget, PROP_TEXT, ((MenuItem) e.widget).getText());
                    }
                };

                for (UIElementAlignment alignment : UIElementAlignment.values()) {
                    final TextWithDropDown text = new TextWithDropDown(parent, SWT.BORDER, alignment.getStyle(), menuSelectionListener);
                    text.getTextComponent().addModifyListener(textModifyListener);
                    text.setData(alignment);
                    text.addMenuItem(CoreMessages.pref_page_ui_general_boolean_predefined_styles, null, null, null).setEnabled(false);
                    text.addMenuSeparator();
                    for (String variant : state.getPredefinedTextStyles()) {
                        text.addMenuItem(variant);
                    }

                    ((GridData) text.getLayoutData()).widthHint = 120;

                    addPropertyChangeListener(event -> {
                        switch (event.getProperty()) {
                            case PROP_TEXT:
                                text.getTextComponent().setText((String) event.getNewValue());
                                break;
                            case PROP_MODE:
                                UIUtils.enableWithChildren(text, event.getNewValue() == BooleanMode.TEXT);
                                break;
                            case PROP_COLOR:
                                text.getTextComponent().setForeground(UIUtils.getSharedColor((RGB) event.getNewValue()));
                                break;
                            case PROP_ALIGN:
                                UIUtils.setControlVisible(text, event.getNewValue() == text.getData());
                                text.getTextComponent().setText(currentText);
                                break;
                        }
                    });
                }
            }

            UIUtils.createLabel(parent, UIIcon.SEPARATOR_V);

            {
                final ToolBar alignToolBar = new ToolBar(parent, SWT.HORIZONTAL);
                alignToolBar.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

                final SelectionListener selectionListener = new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (((ToolItem) e.widget).getSelection()) {
                            // React only to 'Selection' (avoid firing secondary event to 'DefaultSelection')
                            notifyPropertyChanged(e.widget, PROP_ALIGN, e.widget.getData());
                        }
                    }
                };

                for (final UIElementAlignment alignment : UIElementAlignment.values()) {
                    final ToolItem item = new ToolItem(alignToolBar, SWT.RADIO);
                    item.setImage(DBeaverIcons.getImage(alignment.getIcon()));
                    item.setToolTipText(alignment.getLabel());
                    item.addSelectionListener(selectionListener);
                    item.setData(alignment);

                    addPropertyChangeListener(event -> {
                        if (event.getProperty().equals(PROP_ALIGN)) {
                            item.setSelection(event.getNewValue() == item.getData());
                        }
                    });
                }
            }

            UIUtils.createLabel(parent, UIIcon.SEPARATOR_V);

            {
                final DefaultColorSelector selector = new DefaultColorSelector(parent);
                selector.setColorValue(new RGB(0, 0, 0));
                selector.setDefaultColorValue(new RGB(0, 0, 0));
                selector.addListener(e -> notifyPropertyChanged(selector, PROP_COLOR, selector.getColorValue()));

                addPropertyChangeListener(event -> {
                    if (event.getSource() == selector) {
                        // Ignore our own event
                        return;
                    }
                    switch (event.getProperty()) {
                        case PROP_MODE:
                            // Color is only applicable to text-based comboboxes
                            selector.setEnabled(event.getNewValue() == BooleanMode.TEXT);
                            break;
                        case PROP_COLOR:
                            selector.setColorValue((RGB) event.getNewValue());
                            break;
                        case PROP_DEFAULT_COLOR:
                            selector.setDefaultColorValue((RGB) event.getNewValue());
                            break;
                    }
                });
            }

            addPropertyChangeListener(event -> {
                switch (event.getProperty()) {
                    case PROP_MODE:
                        this.currentMode = (BooleanMode) event.getNewValue();
                        break;
                    case PROP_TEXT:
                        this.currentText = (String) event.getNewValue();
                        break;
                    case PROP_ALIGN:
                        this.currentAlignment = (UIElementAlignment) event.getNewValue();
                        this.parent.layout(true);
                        break;
                    case PROP_COLOR:
                        this.currentColor = (RGB) event.getNewValue();
                        break;
                }

                updateBooleanValidState();
            });
        }

        public void loadStyle(@NotNull BooleanStyle style, @NotNull RGB defaultColor) {
            if (style.getMode() == BooleanMode.TEXT) {
                notifyPropertyChanged(this, PROP_TEXT, style.getText());
                notifyPropertyChanged(this, PROP_COLOR, style.getColor());
                notifyPropertyChanged(this, PROP_DEFAULT_COLOR, defaultColor);
            } else {
                notifyPropertyChanged(this, PROP_TEXT, "");
            }

            notifyPropertyChanged(this, PROP_ALIGN, style.getAlignment());
            notifyPropertyChanged(this, PROP_MODE, style.getMode());
        }

        @NotNull
        public BooleanStyle saveStyle() {
            if (currentMode == BooleanMode.TEXT) {
                return BooleanStyle.usingText(currentText, currentAlignment, currentColor);
            } else {
                return BooleanStyle.usingIcon(state.getIcon(), currentAlignment);
            }
        }

        public boolean isValid() {
            return currentMode == BooleanMode.ICON || !CommonUtils.isEmptyTrimmed(currentText);
        }

        public void addPropertyChangeListener(@NotNull IPropertyChangeListener listener) {
            addListenerObject(listener);
        }

        public void notifyPropertyChanged(@NotNull Object source, @NotNull String property, @Nullable Object value) {
            final Object[] listeners = getListeners();
            if (listeners.length > 0) {
                final PropertyChangeEvent event = new PropertyChangeEvent(source, property, null, value);
                for (Object listener : listeners) {
                    ((IPropertyChangeListener) listener).propertyChange(event);
                }
            }
        }
    }
}