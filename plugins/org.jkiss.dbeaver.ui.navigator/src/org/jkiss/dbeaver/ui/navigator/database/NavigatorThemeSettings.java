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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.ui.ThemeColor;
import org.jkiss.dbeaver.ui.ThemeListener;

/**
 * Theme settings
 */
public class NavigatorThemeSettings extends ThemeListener {

    public static final String HINT_FOREGROUND_COLOR = "org.jkiss.dbeaver.ui.navigator.node.foreground";
    public static final String TABLE_STATISTICS_BACKGROUND_COLOR = "org.jkiss.dbeaver.ui.navigator.node.statistics.background";
    public static final String COLOR_NODE_TRANSIENT_FOREGROUND = "org.jkiss.dbeaver.ui.navigator.node.transient.foreground";

    @ThemeColor(HINT_FOREGROUND_COLOR)
    public volatile Color hintColor;
    @ThemeColor(TABLE_STATISTICS_BACKGROUND_COLOR)
    public volatile Color statisticsFrameColor;
    @ThemeColor(COLOR_NODE_TRANSIENT_FOREGROUND)
    public volatile Color transientForeground;

    public static final NavigatorThemeSettings instance = new NavigatorThemeSettings();
}
