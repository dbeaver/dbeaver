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
package org.jkiss.dbeaver.ui.css;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.e4.ui.css.swt.helpers.SWTElementHelpers;
import org.eclipse.e4.ui.css.swt.properties.css2.CSSPropertyBackgroundSWTHandler;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;


/**
 * Needed to override theme styles.
 * For now it's used only for coloring widgets regarding the connection type color.
 */
public class CustomCompositeElementHandler extends CSSPropertyBackgroundSWTHandler {

    public static final String PROP_BACKGROUND_COLOR = "background-color";
    public static final String PROP_COLOR = "color";
    private static final boolean APPLY_CON_TYPE_HIERARCHICALLY = false;

    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine) throws Exception {
        if (property.equals(PROP_BACKGROUND_COLOR) && UIStyles.isDarkTheme()) {
            Composite widget = (Composite) SWTElementHelpers.getWidget(element);
            Widget mimicControl = CSSUtils.getMimicControl(widget);
            if (mimicControl != null && mimicControlStyles(engine, mimicControl, widget)) {
                return true;
            }
        }
        return super.applyCSSProperty(element, property, value, pseudo, engine);
    }

    private static boolean mimicControlStyles(CSSEngine engine, Widget mimicControl, Composite widget) throws Exception {
        Element mimicElement = engine.getElement(mimicControl);
        if (mimicElement != null) {
            CSSStyleDeclaration computedStyle = engine.getViewCSS().getComputedStyle(mimicElement, null);
            if (computedStyle != null) {
                Color bgColor = (Color) engine.convert(
                    computedStyle.getPropertyCSSValue(PROP_BACKGROUND_COLOR),
                    Color.class,
                    widget.getDisplay());
                widget.setBackground(bgColor);
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyCSSPropertyBackgroundColor(
        Object element,
        CSSValue value,
        String pseudo,
        CSSEngine engine
    ) throws Exception {
        Widget widget = SWTElementHelpers.getWidget(element);
        if (widget == null || (widget instanceof Control && UIUtils.isInDialog((Control) widget))) {
            super.applyCSSPropertyBackgroundColor(element, value, pseudo, engine);
            return;
        }

        if (widget instanceof Control ctrl && !(ctrl instanceof StyledText)) {
            boolean colorByConnectionType = false;
            if (APPLY_CON_TYPE_HIERARCHICALLY) {
                for (Control c = ctrl; c != null; c = c.getParent()) {
                    if (DBStyles.COLORED_BY_CONNECTION_TYPE.equals(CSSUtils.getCSSClass(c))) {
                        colorByConnectionType = true;
                        break;
                    }
                }
            } else {
                colorByConnectionType = DBStyles.COLORED_BY_CONNECTION_TYPE.equals(CSSUtils.getCSSClass(widget));
            }

            if (colorByConnectionType) {
                Color newColor = CSSUtils.getCurrentEditorConnectionColor(widget);
                if (newColor != null) {
                    applyCustomBackground(element, newColor);
                    return;
                }
            }
        }
        super.applyCSSPropertyBackgroundColor(element, value, pseudo, engine);
    }

    protected void applyCustomBackground(Object element, Color newColor) {
        Composite nativeWidget = (Composite) ((CompositeElement) element).getNativeWidget();
        nativeWidget.setBackground(newColor);
    }


}
