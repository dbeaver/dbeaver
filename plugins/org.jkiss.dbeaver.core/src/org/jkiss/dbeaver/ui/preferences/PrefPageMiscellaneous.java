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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.DefaultColorSelector;
import org.jkiss.dbeaver.ui.controls.TextWithDropDown;
import org.jkiss.dbeaver.ui.controls.bool.BooleanMode;
import org.jkiss.dbeaver.ui.controls.bool.BooleanState;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyle;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleSet;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PrefPageMiscellaneous extends PrefPageMiscellaneousAbstract implements IWorkbenchPreferencePage {
    private Button holidayDecorationsCheck;
    private final List<Consumer<BooleanStyleSet>> booleanStylesChangeListeners = new ArrayList<>();
    private BooleanPanel booleanCheckedPanel;
    private BooleanPanel booleanUncheckedPanel;
    private BooleanPanel booleanNullPanel;
    private RGB defaultColor;

    public PrefPageMiscellaneous() {
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected Object getConfiguratorObject() {
        return super.getConfiguratorObject();
    }

    @Override
    public void init(IWorkbench workbench) {
        // nothing to init
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_boolean, 3, GridData.FILL_HORIZONTAL, 0);

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

            BooleanStyleSet savedStyles = BooleanStyleSet.getDefaultStyles(DBWorkbench.getPlatform().getPreferenceStore());
            BooleanStyleSet defaultStyles = BooleanStyleSet.getDefaultStyleSet();

            booleanCheckedPanel = new BooleanPanel(
                    group,
                    BooleanState.CHECKED,
                    savedStyles.getStyle(BooleanState.CHECKED).getMode() == BooleanMode.TEXT ?
                            savedStyles.getStyle(BooleanState.CHECKED).getText() :
                            defaultStyles.getStyle(BooleanState.CHECKED).getText()
            );

            booleanUncheckedPanel = new BooleanPanel(
                    group,
                    BooleanState.UNCHECKED,
                    savedStyles.getStyle(BooleanState.UNCHECKED).getMode() == BooleanMode.TEXT ?
                            savedStyles.getStyle(BooleanState.UNCHECKED).getText() :
                            defaultStyles.getStyle(BooleanState.UNCHECKED).getText()
            );

            booleanNullPanel = new BooleanPanel(
                    group,
                    BooleanState.NULL,
                    savedStyles.getStyle(BooleanState.NULL).getMode() == BooleanMode.TEXT ?
                            savedStyles.getStyle(BooleanState.NULL).getText() :
                            defaultStyles.getStyle(BooleanState.NULL).getText()
            );

            notifyBooleanStylesChanged(savedStyles);
        }

        {
            final Group group = UIUtils.createControlGroup(composite, "Holiday decorations", 1, GridData.FILL_HORIZONTAL, 0);

            holidayDecorationsCheck = UIUtils.createCheckbox(group, "Show holiday decorations", false);
            holidayDecorationsCheck.setLayoutData(new GridData());
            holidayDecorationsCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS));
            UIUtils.createInfoLabel(group, CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart);
        }

        injectConfigurators(composite);

        return composite;
    }

    @Override
    protected void performDefaults() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        holidayDecorationsCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS));

        notifyBooleanStylesChanged(BooleanStyleSet.getDefaultStyleSet());

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS, holidayDecorationsCheck.getSelection());

        BooleanStyleSet.setDefaultStyles(store, new BooleanStyleSet(
            booleanCheckedPanel.saveStyle(),
            booleanUncheckedPanel.saveStyle(),
            booleanNullPanel.saveStyle(),
            defaultColor
        ));

        return super.performOk();
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
        private static final String PROP_FONT = "fontValue";
        private static final String PROP_TEXT = "textValue";
        private static final String PROP_ALIGN = "alignValue";
        private static final String PROP_COLOR = "colorValue";
        private static final String PROP_DEFAULT_COLOR = "defaultColorValue";

        private static final int MENU_PRESET_ID = 1;
        private static final int MENU_FONT_ID = 2;
        private static final int MENU_RESET_COLOR_ID = 3;

        private final Composite parent;
        private final BooleanState state;

        private final Font normalFont;
        private final Font boldFont;
        private final Font italicFont;

        private BooleanMode currentMode;
        private String currentText;
        private String savedText;
        private UIElementAlignment currentAlignment;
        private UIElementFontStyle currentFontStyle;
        private RGB currentColor;
        private RGB currentDefaultColor;

        public BooleanPanel(@NotNull Composite parent, @NotNull BooleanState state, @NotNull String savedText) {
            this.parent = parent;
            this.state = state;
            this.savedText = savedText;

            final FontDescriptor fontDescriptor = FontDescriptor.createFrom(parent.getFont());
            this.normalFont = parent.getFont();
            this.boldFont = fontDescriptor.setStyle(SWT.BOLD).createFont(parent.getDisplay());
            this.italicFont = fontDescriptor.setStyle(SWT.ITALIC).createFont(parent.getDisplay());

            parent.addDisposeListener(e -> {
                UIUtils.dispose(boldFont);
                UIUtils.dispose(italicFont);
            });

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
                        final MenuItem menu = (MenuItem) e.widget;
                        switch (menu.getID()) {
                            case MENU_PRESET_ID:
                                notifyPropertyChanged(e.widget, PROP_TEXT, menu.getText());
                                break;
                            case MENU_FONT_ID:
                                notifyPropertyChanged(e.widget, PROP_FONT, menu.getData());
                                break;
                            case MENU_RESET_COLOR_ID:
                                notifyPropertyChanged(e.widget, PROP_COLOR, currentDefaultColor);
                                break;
                        }
                    }
                };

                for (UIElementAlignment alignment : UIElementAlignment.values()) {
                    final TextWithDropDown text = new TextWithDropDown(parent, SWT.BORDER, alignment.getStyle(), menuSelectionListener);
                    text.getTextComponent().addModifyListener(textModifyListener);
                    text.setData(alignment);
                    for (String variant : state.getPresets()) {
                        text.addMenuItem(variant).setID(MENU_PRESET_ID);
                    }
                    text.addMenuSeparator();
                    text.addMenuItemWithMenu(CoreMessages.pref_page_ui_general_boolean_styles, null, menu -> {
                        for (UIElementFontStyle value : UIElementFontStyle.values()) {
                            final MenuItem item = text.addMenuItem(menu, value.getLabel(), null, value, SWT.RADIO);
                            item.setID(MENU_FONT_ID);

                            addPropertyChangeListener(event -> {
                                if (event.getProperty().equals(PROP_FONT)) {
                                    item.setSelection(event.getNewValue() == item.getData());
                                }
                            });
                        }
                    });
                    text.addMenuItemWithMenu(CoreMessages.pref_page_ui_general_boolean_color, null, menu -> {
                        final MenuItem item = text.addMenuItem(menu, CoreMessages.pref_page_ui_general_boolean_color_use_theme_color, null, null, SWT.CHECK);
                        item.setID(MENU_RESET_COLOR_ID);
                        addPropertyChangeListener(event -> {
                            if (event.getProperty().equals(PROP_COLOR)) {
                                item.setSelection(event.getNewValue() == currentDefaultColor);
                            }
                        });
                    });

                    ((GridData) text.getLayoutData()).widthHint = 120;

                    addPropertyChangeListener(event -> {
                        switch (event.getProperty()) {
                            case PROP_TEXT:
                                text.getTextComponent().setText((String) event.getNewValue());
                                break;
                            case PROP_FONT:
                                switch ((UIElementFontStyle) event.getNewValue()) {
                                    case NORMAL:
                                        text.getTextComponent().setFont(normalFont);
                                        break;
                                    case ITALIC:
                                        text.getTextComponent().setFont(italicFont);
                                        break;
                                    case BOLD:
                                        text.getTextComponent().setFont(boldFont);
                                        break;
                                }
                                break;
                            case PROP_MODE:
                                UIUtils.enableWithChildren(text, event.getNewValue() == BooleanMode.TEXT);
                                if (event.getNewValue() == BooleanMode.TEXT) {
                                    text.getTextComponent().setText(this.savedText);
                                } else {
                                    text.getTextComponent().setText("");
                                }
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
                final DefaultColorSelector selector = new DefaultColorSelector(parent, false);
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
                    case PROP_FONT:
                        this.currentFontStyle = (UIElementFontStyle) event.getNewValue();
                        break;
                    case PROP_COLOR:
                        this.currentColor = (RGB) event.getNewValue();
                        break;
                    case PROP_DEFAULT_COLOR:
                        this.currentDefaultColor = (RGB) event.getNewValue();
                        break;
                }

                updateBooleanValidState();
            });
        }

        public void loadStyle(@NotNull BooleanStyle style, @NotNull RGB defaultColor) {
            if (style.getMode() == BooleanMode.TEXT) {
                notifyPropertyChanged(this, PROP_TEXT, style.getText());
                notifyPropertyChanged(this, PROP_FONT, style.getFontStyle());
                notifyPropertyChanged(this, PROP_DEFAULT_COLOR, defaultColor);
                notifyPropertyChanged(this, PROP_COLOR, style.getColor());
            } else {
                notifyPropertyChanged(this, PROP_TEXT, "");
            }

            notifyPropertyChanged(this, PROP_ALIGN, style.getAlignment());
            notifyPropertyChanged(this, PROP_MODE, style.getMode());
        }

        @NotNull
        public BooleanStyle saveStyle() {
            if (currentMode == BooleanMode.TEXT) {
                savedText = currentText;
                return BooleanStyle.usingText(currentText, currentAlignment, currentColor, currentFontStyle);
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
