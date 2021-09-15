/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.bool;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIElementAlignment;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.utils.CommonUtils;

public class BooleanStyleSet {
    private static final String PREF_BOOLEAN_STYLE = "ui.render.boolean.style";

    /* For backward compatibility with previous versions. Maps old presets into new style. */
    private static final String PROP_LEGACY_STYLE_ICON = "ICON";
    private static final String PROP_LEGACY_STYLE_TEXTBOX = "TEXTBOX";
    private static final String PROP_LEGACY_STYLE_CHECKBOX = "CHECKBOX";
    private static final String PROP_LEGACY_STYLE_TRUE_FALSE = "TRUE_FALSE";
    private static final String PROP_LEGACY_STYLE_YES_NO = "YES_NO";

    private static final String PROP_MODE = "mode";
    private static final String PROP_TEXT = "text";
    private static final String PROP_ALIGN = "align";
    private static final String PROP_COLOR = "color";
    private static final String COLOR_DEFAULT = "default";

    private final BooleanStyle checkedStyle;
    private final BooleanStyle uncheckedStyle;
    private final BooleanStyle nullStyle;
    private final RGB defaultColor;

    public BooleanStyleSet(@NotNull BooleanStyle checkedStyle, @NotNull BooleanStyle uncheckedStyle, @NotNull BooleanStyle nullStyle, @NotNull RGB defaultColor) {
        this.checkedStyle = checkedStyle;
        this.uncheckedStyle = uncheckedStyle;
        this.nullStyle = nullStyle;
        this.defaultColor = defaultColor;

        Assert.isLegal(checkedStyle.getMode() == uncheckedStyle.getMode() && uncheckedStyle.getMode() == nullStyle.getMode(), "Mixed style modes");
    }

    @NotNull
    public BooleanStyle getStyle(@Nullable Boolean value) {
        return value == null ? nullStyle : value ? checkedStyle : uncheckedStyle;
    }

    @NotNull
    public BooleanStyle getCheckedStyle() {
        return checkedStyle;
    }

    @NotNull
    public BooleanStyle getUncheckedStyle() {
        return uncheckedStyle;
    }

    @NotNull
    public BooleanStyle getNullStyle() {
        return nullStyle;
    }

    @NotNull
    public BooleanMode getMode() {
        return checkedStyle.getMode();
    }

    @NotNull
    public RGB getDefaultColor() {
        return defaultColor;
    }

    public static BooleanStyleSet getDefaultStyles(@NotNull DBPPreferenceStore store) {
        final BooleanMode mode = CommonUtils.valueOf(BooleanMode.class, store.getString(PREF_BOOLEAN_STYLE + ".mode"));
        return getDefaultStyles(store, mode);
    }

    @NotNull
    public static BooleanStyleSet getDefaultStyles(@NotNull DBPPreferenceStore store, @Nullable BooleanMode mode) {
        final RGB defaultColor = UIStyles.getDefaultTextForeground().getRGB();

        if (mode != null) {
            return new BooleanStyleSet(
                getDefaultStyle(store, mode, BooleanState.CHECKED, defaultColor),
                getDefaultStyle(store, mode, BooleanState.UNCHECKED, defaultColor),
                getDefaultStyle(store, mode, BooleanState.NULL, defaultColor),
                defaultColor
            );
        } else {
            return new BooleanStyleSet(
                getDefaultStyleLegacy(store, BooleanState.CHECKED, defaultColor),
                getDefaultStyleLegacy(store, BooleanState.UNCHECKED, defaultColor),
                getDefaultStyleLegacy(store, BooleanState.NULL, defaultColor),
                defaultColor
            );
        }
    }

    public static void setDefaultStyles(@NotNull DBPPreferenceStore store, @NotNull BooleanStyleSet set) {
        store.setValue(PREF_BOOLEAN_STYLE + '.' + PROP_MODE, set.getMode().name());
        setDefaultStyle(store, set.getCheckedStyle(), BooleanState.CHECKED, set.getDefaultColor());
        setDefaultStyle(store, set.getUncheckedStyle(), BooleanState.UNCHECKED, set.getDefaultColor());
        setDefaultStyle(store, set.getNullStyle(), BooleanState.NULL, set.getDefaultColor());
    }

    public static void installStyleChangeListener(@NotNull Control control, @NotNull IPropertyChangeListener listener) {
        final DBPPreferenceListener preferenceListener = e -> {
            listener.propertyChange(new PropertyChangeEvent(e.getSource(), e.getProperty(), e.getOldValue(), e.getNewValue()));
        };

        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(listener);
        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(preferenceListener);
        DBWorkbench.getPlatform().getDataSourceProviderRegistry().getGlobalDataSourcePreferenceStore().addPropertyChangeListener(preferenceListener);

        control.addDisposeListener(e -> {
            DBWorkbench.getPlatform().getDataSourceProviderRegistry().getGlobalDataSourcePreferenceStore().removePropertyChangeListener(preferenceListener);
            DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(preferenceListener);
            PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(listener);
        });
    }

    @NotNull
    private static BooleanStyle getDefaultStyle(@NotNull DBPPreferenceStore store, @NotNull BooleanMode mode, @NotNull BooleanState state, @NotNull RGB defaultColor) {
        final String namespace = PREF_BOOLEAN_STYLE + '.' + state.getId() + '.';

        if (mode == BooleanMode.TEXT) {
            final String text = store.getString(namespace + PROP_TEXT);
            final UIElementAlignment alignment = CommonUtils.valueOf(UIElementAlignment.class, store.getString(namespace + PROP_ALIGN), UIElementAlignment.CENTER);
            final RGB color = convertStringToColor(store.getString(namespace + PROP_COLOR), defaultColor);
            return BooleanStyle.usingText(text.trim(), alignment, color);
        } else {
            final UIElementAlignment alignment = CommonUtils.valueOf(UIElementAlignment.class, store.getString(namespace + PROP_ALIGN), UIElementAlignment.CENTER);
            return BooleanStyle.usingIcon(state.getIcon(), alignment);
        }
    }

    @NotNull
    private static BooleanStyle getDefaultStyleLegacy(@NotNull DBPPreferenceStore store, @NotNull BooleanState state, @NotNull RGB color) {
        switch (store.getString(PREF_BOOLEAN_STYLE)) {
            case PROP_LEGACY_STYLE_ICON:
                return BooleanStyle.usingIcon(state.choose(UIIcon.CHECK_ON, UIIcon.CHECK_OFF, UIIcon.CHECK_QUEST), UIElementAlignment.CENTER);
            case PROP_LEGACY_STYLE_CHECKBOX:
                return BooleanStyle.usingText(state.choose("☑", "☐", "☒"), UIElementAlignment.CENTER, color);
            case PROP_LEGACY_STYLE_TRUE_FALSE:
                return BooleanStyle.usingText(state.choose("true", "false", DBConstants.NULL_VALUE_LABEL), UIElementAlignment.CENTER, color);
            case PROP_LEGACY_STYLE_YES_NO:
                return BooleanStyle.usingText(state.choose("yes", "no", DBConstants.NULL_VALUE_LABEL), UIElementAlignment.CENTER, color);
            case PROP_LEGACY_STYLE_TEXTBOX:
            default:
                return BooleanStyle.usingText(state.choose("[v]", "[ ]", DBConstants.NULL_VALUE_LABEL), UIElementAlignment.CENTER, color);
        }
    }

    private static void setDefaultStyle(@NotNull DBPPreferenceStore store, @NotNull BooleanStyle style, @NotNull BooleanState state, @NotNull RGB defaultColor) {
        final String namespace = PREF_BOOLEAN_STYLE + '.' + state.getId() + '.';

        if (style.getMode() == BooleanMode.TEXT) {
            store.setValue(namespace + PROP_TEXT, style.getText().trim());
            store.setValue(namespace + PROP_ALIGN, style.getAlignment().name());
            store.setValue(namespace + PROP_COLOR, convertColorToString(style.getColor(), defaultColor));
        } else {
            store.setValue(namespace + PROP_ALIGN, style.getAlignment().name());
        }
    }

    @NotNull
    private static String convertColorToString(@NotNull RGB color, @NotNull RGB defaultColor) {
        return color == defaultColor ? COLOR_DEFAULT : StringConverter.asString(color);
    }

    @NotNull
    private static RGB convertStringToColor(@NotNull String color, @NotNull RGB defaultColor) {
        return COLOR_DEFAULT.equals(color) ? defaultColor : StringConverter.asRGB(color, defaultColor);
    }
}
