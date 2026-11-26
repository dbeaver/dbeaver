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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * Theme settings
 */
public class BaseThemeSettings extends ThemeListener {
    public static final String ICON_DEFAULT = "org.jkiss.dbeaver.ui.icons.default";
    public static final String ICON_SECONDARY = "org.jkiss.dbeaver.ui.icons.secondary";
    public static final String ICON_ACCENT = "org.jkiss.dbeaver.ui.icons.accent";
    public static final String ICON_TIP = "org.jkiss.dbeaver.ui.icons.tip";
    public static final String ICON_ATTENTION = "org.jkiss.dbeaver.ui.icons.attention";
    public static final String ICON_POSITIVE = "org.jkiss.dbeaver.ui.icons.positive";
    public static final String ICON_IMPORTANT = "org.jkiss.dbeaver.ui.icons.important";
    public static final String ICON_DANGER = "org.jkiss.dbeaver.ui.icons.danger";
    public static final String ICON_BACKGROUND = "org.jkiss.dbeaver.ui.icons.background";

    @ThemeColor(ICON_DEFAULT)
    public volatile Color iconDefault;
    @ThemeColor(ICON_SECONDARY)
    public volatile Color iconSecondary;
    @ThemeColor(ICON_ACCENT)
    public volatile Color iconAccent;
    @ThemeColor(ICON_TIP)
    public volatile Color iconTip;
    @ThemeColor(ICON_ATTENTION)
    public volatile Color iconAttention;
    @ThemeColor(ICON_POSITIVE)
    public volatile Color iconPositive;
    @ThemeColor(ICON_IMPORTANT)
    public volatile Color iconImportant;
    @ThemeColor(ICON_DANGER)
    public volatile Color iconDanger;
    @ThemeColor(ICON_BACKGROUND)
    public volatile Color iconBackground;

    @ThemeFont(value = UIFonts.DBeaver.MAIN_FONT)
    public volatile Font baseFont;
    @ThemeFont(value = UIFonts.DBeaver.MAIN_FONT, bold = true)
    public volatile Font baseFontBold;
    @ThemeFont(value = UIFonts.DBeaver.MAIN_FONT, italic = true)
    public volatile Font baseFontItalic;

    @ThemeFont(value = UIFonts.Eclipse.TREE_AND_TABLE_FONT_FOR_VIEWS)
    public volatile Font treeAndTableFont;
    @ThemeFont(value = UIFonts.Eclipse.TREE_AND_TABLE_FONT_FOR_VIEWS, bold = true)
    public volatile Font treeAndTableFontBold;
    @ThemeFont(value = UIFonts.Eclipse.TREE_AND_TABLE_FONT_FOR_VIEWS, italic = true)
    public volatile Font treeAndTableFontItalic;

    @ThemeFont(value = UIFonts.Eclipse.PART_TITLE_FONT)
    public volatile Font partTitleFont;
    @ThemeFont(value = UIFonts.Eclipse.PART_TITLE_FONT, bold = true)
    public volatile Font partTitleBoldFont;

    @ThemeFont(UIFonts.DBeaver.MONOSPACE_FONT)
    public volatile Font monospaceFont;

    @ThemeColor(BaseEditorColors.COLOR_SUCCESS)
    public volatile Color colorSuccess;
    @ThemeColor(BaseEditorColors.COLOR_ERROR)
    public volatile Color colorError;
    @ThemeColor(BaseEditorColors.COLOR_WARNING)
    public volatile Color colorWarning;
    @ThemeColor(BaseEditorColors.COLOR_WARNING_FOREGROUND)
    public volatile Color colorWarningForeground;
    @ThemeColor(BaseEditorColors.COLOR_ERROR_FOREGROUND)
    public volatile Color colorErrorForeground;

    @ThemeColor(BaseEditorColors.COLOR_ACCENT)
    public volatile Color colorAccent;

    @ThemeColor(BaseEditorColors.COLOR_BACKGROUND_BLUE)
    public volatile Color colorBackgroundBlue;
    @ThemeColor(BaseEditorColors.COLOR_BACKGROUND_YELLOW)
    public volatile Color colorBackgroundYellow;
    @ThemeColor(BaseEditorColors.COLOR_BACKGROUND_GREEN)
    public volatile Color colorBackgroundGreen;

    public static final BaseThemeSettings instance = new BaseThemeSettings();
}
