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
package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.e4.ui.css.swt.dom.CTabFolderElement;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.custom.CSSPropertye4SelectedTabFillHandler;
import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.ui.UIUtils;
import org.w3c.dom.css.CSSValue;

/**
 * Needed to override theme styles.
 * For now it's used only for coloring widgets regarding the connection type color.
 */
public class CustomSelectedTabFillHandler extends CSSPropertye4SelectedTabFillHandler {


    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
            throws Exception {

        Widget widget = SWTElementHelpers.getWidget(element);
        if (widget == null || (widget instanceof Control && UIUtils.isInDialog((Control)widget))) {
            return false;
        }

        if (DBStyles.COLORED_BY_CONNECTION_TYPE.equals(widget.getData(CSSSWTConstants.CSS_CLASS_NAME_KEY))) {
            Color newColor = CSSUtils.getCurrentEditorConnectionColor(widget);
            if (newColor != null) {
                CTabFolder nativeWidget = (CTabFolder) ((CTabFolderElement) element).getNativeWidget();
                if (nativeWidget.getRenderer() instanceof CTabRendering) {
                    ((CTabRendering) nativeWidget.getRenderer()).setSelectedTabFill(newColor);
                } else {
                    nativeWidget.setBackground(newColor);
                }
                return true;
            }
        }
        return super.applyCSSProperty(element, property, value, pseudo, engine);

    }

}
