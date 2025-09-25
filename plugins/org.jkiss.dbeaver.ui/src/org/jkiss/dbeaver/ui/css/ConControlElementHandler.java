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
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.css2.CSSPropertyBackgroundSWTHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.ConComposite;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.w3c.dom.css.CSSValue;

public class ConControlElementHandler extends CSSPropertyBackgroundSWTHandler {

    private static final Class<?>[] EXCLUDE_CLASSES = { Tree.class, Table.class };

    @Override
    public void applyCSSPropertyBackgroundColor(
        Object element,
        CSSValue value,
        String pseudo,
        CSSEngine engine
    ) throws Exception {
        Widget widget = SWTElementHelpers.getWidget(element);

        super.applyCSSPropertyBackgroundColor(element, value, pseudo, engine);

        if (widget instanceof ToolBar toolBar) {
            // FIXME: it is a hack to set toolbar foreground explicitly.
            // FIXME: For some reason it remains default for dark theme (black on black)
            Color defForeground = UIStyles.getDefaultTextForeground();
            toolBar.setForeground(defForeground);
            Color bgColor = CSSUtils.getCurrentEditorConnectionColor(widget);
            if (bgColor != null) {
                toolBar.setBackground(bgColor);
            }
            return;
        }

        if (widget instanceof Control ctrl &&
            !UIUtils.isInDialog(ctrl) &&
            !isExcludedFromStyling(ctrl) &&
            isOverridesBackground(ctrl) &&
            !CompositeElement.hasBackgroundOverriddenByCSS(ctrl)
        ) {
            Color newColor = CSSUtils.getCurrentEditorConnectionColor(widget);
            if (newColor != null) {
                ctrl.setBackground(newColor);
            }
        }
    }

    private static boolean isExcludedFromStyling(Control ctrl) {
        if (ArrayUtils.contains(EXCLUDE_CLASSES, ctrl.getClass()) || CSSUtils.isExcludeFromStyling(ctrl)) {
            return true;
        }
        if (ctrl instanceof Text || ctrl instanceof StyledText) {
            return CommonUtils.isBitSet(ctrl.getStyle(), SWT.BORDER);
        }
        if (ctrl instanceof Button) {
            return !CommonUtils.isBitSet(ctrl.getStyle(), SWT.CHECK) && !CommonUtils.isBitSet(ctrl.getStyle(), SWT.RADIO);
        }

        if (CompositeElement.hasBackgroundOverriddenByCSS(ctrl)) {
            if (ctrl instanceof Combo || ctrl instanceof CCombo) {
                return true;
            }
        }

        return false;
    }

    private boolean isOverridesBackground(@NotNull Control control) {
        while (control != null) {
            if (CSSUtils.isExcludeFromStyling(control)) {
                return false;
            }
            if (control.getClass().getName().contains("FindReplaceOverlay")) {
                // FIXME: dirty hack to exclude Find/Replace floating panel
                return false;
            }
            // Should we use def Eclipse approach? Generally it is all canvases and composites
//            if (CompositeElement.hasBackgroundOverriddenByCSS(control)) {
//                return false;
//            }
            if (control instanceof ConComposite || CSSUtils.isDatabaseColored(control)) {
                return true;
            }
            control = control.getParent();
        }
        return false;
    }

}
