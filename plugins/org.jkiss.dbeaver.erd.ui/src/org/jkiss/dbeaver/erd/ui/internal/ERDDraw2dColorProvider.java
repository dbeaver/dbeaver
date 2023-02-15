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
package org.jkiss.dbeaver.erd.ui.internal;

import org.eclipse.draw2dl.ColorProviderLegacy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class ERDDraw2dColorProvider extends ColorProviderLegacy {
    @Override
    public Color getButton() {
        return UIStyles.getDefaultWidgetBackground();
    }

    @Override
    public Color getListBackground() {
        return UIStyles.getDefaultTextBackground();
    }

    @Override
    public Color getListForeground() {
        return UIStyles.getDefaultTextForeground();
    }

    @Override
    public Color getMenuBackground() {
        return UIStyles.getDefaultWidgetBackground();
    }

    @Override
    public Color getMenuForeground() {
        return UIStyles.getDefaultTextForeground();
    }

    @Override
    public Color getMenuBackgroundSelected() {
        return UIStyles.getDefaultTextSelectionBackground();
    }

    @Override
    public Color getMenuForegroundSelected() {
        return UIStyles.getDefaultTextSelectionForeground();
    }

    @Override
    public Color getLineForeground() {
        return UIStyles.isDarkTheme() ?
            UIUtils.getSharedColor(new RGB(64, 64, 64)) :
            getColor(SWT.COLOR_GRAY);
    }

    @Override
    public Color getListHoverBackgroundColor() {
        return UIStyles.isDarkTheme() ? getColor(SWT.COLOR_WIDGET_NORMAL_SHADOW) : super.getListHoverBackgroundColor();
    }

    @Override
    public Color getListSelectedBackgroundColor() {
        return UIStyles.isDarkTheme() ? getColor(SWT.COLOR_WIDGET_DARK_SHADOW) : super.getListSelectedBackgroundColor();
    }
}
