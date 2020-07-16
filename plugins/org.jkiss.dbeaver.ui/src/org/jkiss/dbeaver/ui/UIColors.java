/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.graphics.RGB;

/**
 * ERD constants
 */
public class UIColors {


    public static final RGB[] EXTRA_DS_COLORS = new RGB[] {
        new RGB(119, 206, 130),
        new RGB(206, 63, 34),
        new RGB(242, 132, 35),
        new RGB(124, 38, 19),
        new RGB(157, 214, 245),
        new RGB(173, 140, 127),
        new RGB(249, 214, 205)
    };

    public static final RGB[] EXTRA_COLORS_FOR_TABLES = new RGB[] {
        new RGB(50, 200, 50),
        new RGB(50, 200, 200),
        new RGB(200, 80, 80),
        new RGB(200, 50, 200),
        new RGB(200, 170, 50),
        new RGB(200, 200, 200),
        new RGB(0, 50, 200)
    };

    public static Color getColor(int index) {
        RGB[] extraDsColors = UIColors.EXTRA_DS_COLORS;
        if (index >= extraDsColors.length) {
            index = index % extraDsColors.length;
        }
        return UIUtils.getSharedColor(extraDsColors[index]);
    }

    public static Color getColorForTable(int index) {
        RGB[] extraColorsForTables = UIColors.EXTRA_COLORS_FOR_TABLES;
        if (index >= extraColorsForTables.length) {
            index = index % extraColorsForTables.length;
        }
        return UIUtils.getSharedColor(extraColorsForTables[index]);
    }

}
