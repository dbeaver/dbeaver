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

package org.jkiss.dbeaver.erd.ui.notations;

import org.eclipse.draw2d.Label;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;

public abstract class ERDNotationBase {
    protected static final String LABEL_0_TO_1 = "0..1";
    protected static final String LABEL_1 = "1";
    protected static final String LABEL_1_TO_N = "1..n";
    protected static final int LBL_V_DISTANCE = -4;
    protected static final int LBL_U_DISTANCE = 3;
    protected static final int CIRCLE_RADIUS = 4;

    protected Font getFont() {
        final IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
        return themeManager.getCurrentTheme().getFontRegistry().get(ERDUIConstants.PROP_DIAGRAM_NOTATION_LABEL_FONT);
    }

    protected Label getLabel(String name, Color frgColor) {
        Label label = new Label(name);
        label.setFont(getFont());
        label.setForegroundColor(frgColor);
        return label;
    }
}
