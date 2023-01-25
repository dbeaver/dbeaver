/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.swt.graphics.RGB;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIElementAlignment;
import org.jkiss.dbeaver.ui.UIElementFontStyle;

public class BooleanStyle {
    private final BooleanMode mode;
    private final String text;
    private final DBIcon icon;
    private final UIElementAlignment alignment;
    private final UIElementFontStyle fontStyle;
    private final RGB color;

    private BooleanStyle(@NotNull BooleanMode mode, @Nullable String text, @Nullable DBIcon icon, @NotNull UIElementAlignment alignment, @Nullable UIElementFontStyle fontStyle, @Nullable RGB color) {
        this.mode = mode;
        this.text = text;
        this.icon = icon;
        this.alignment = alignment;
        this.fontStyle = fontStyle;
        this.color = color;

        Assert.isLegal(mode != BooleanMode.TEXT || (text != null && icon == null && fontStyle != null && color != null), "Only text, color and font must be present in text style");
        Assert.isLegal(mode != BooleanMode.ICON || (text == null && icon != null && fontStyle == null && color == null), "Only icon must be present in icon style");
    }

    @NotNull
    public static BooleanStyle usingText(@NotNull String text, @NotNull UIElementAlignment alignment, @NotNull RGB color, @NotNull UIElementFontStyle font) {
        return new BooleanStyle(BooleanMode.TEXT, text, null, alignment, font, color);
    }

    @NotNull
    public static BooleanStyle usingIcon(@NotNull DBIcon icon, @NotNull UIElementAlignment alignment) {
        return new BooleanStyle(BooleanMode.ICON, null, icon, alignment, null, null);
    }

    @NotNull
    public BooleanMode getMode() {
        return mode;
    }

    @NotNull
    public String getText() {
        Assert.isLegal(mode == BooleanMode.TEXT);
        return text;
    }

    @NotNull
    public DBIcon getIcon() {
        Assert.isLegal(mode == BooleanMode.ICON);
        return icon;
    }

    @NotNull
    public UIElementAlignment getAlignment() {
        return alignment;
    }

    @NotNull
    public UIElementFontStyle getFontStyle() {
        Assert.isLegal(mode == BooleanMode.TEXT);
        return fontStyle;
    }

    @NotNull
    public RGB getColor() {
        Assert.isLegal(mode == BooleanMode.TEXT);
        return color;
    }
}
