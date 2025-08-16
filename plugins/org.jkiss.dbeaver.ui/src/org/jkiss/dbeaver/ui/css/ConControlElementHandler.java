/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.css2.CSSPropertyBackgroundSWTHandler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ConComposite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.w3c.dom.css.CSSValue;

public class ConControlElementHandler extends CSSPropertyBackgroundSWTHandler {

    private static final Class<?>[] EXCLUDE_CLASSES = { Tree.class, Table.class, Button.class };

    @Override
    public void applyCSSPropertyBackgroundColor(
        Object element,
        CSSValue value,
        String pseudo,
        CSSEngine engine
    ) throws Exception {
        Widget widget = SWTElementHelpers.getWidget(element);
        if (widget instanceof Control ctrl &&
            !UIUtils.isInDialog(ctrl) &&
            !ArrayUtils.contains(EXCLUDE_CLASSES, ctrl.getClass()) &&
            isOverridesBackground(ctrl)
        ) {
            Color newColor = CSSUtils.getCurrentEditorConnectionColor(widget);
            if (newColor != null) {
                ctrl.setBackground(newColor);
                return;
            }
        }
        super.applyCSSPropertyBackgroundColor(element, value, pseudo, engine);
    }

    private boolean isOverridesBackground(@NotNull Control control) {
        while (control != null) {
            if (control instanceof ConComposite || CSSUtils.isDatabaseColored(control)) {
                return true;
            }
            control = control.getParent();
        }
        return false;
    }

}
