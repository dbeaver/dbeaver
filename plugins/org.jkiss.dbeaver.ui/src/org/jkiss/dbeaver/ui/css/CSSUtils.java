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

import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;

public class CSSUtils {

    public static final String COLORED_BY_CONNECTION_TYPE = "coloredByConnectionType";
    public static final String EXCLUDED_FROM_STYLING = "excludedFromStyling";
    public static final String DATABASE_EDITOR_COMPOSITE_DATASOURCE = "databaseEditorCompositeBackground";

    public static String getCSSClass(Widget widget){
        return (String) widget.getData(CSSSWTConstants.CSS_CLASS_NAME_KEY);
    }

    /**
     * Set value to a widget as a CSSSWTConstants.CSS_CLASS_NAME_KEY value.
     */
    public static void setCSSClass(Widget widget, String value){
        widget.setData(CSSSWTConstants.CSS_CLASS_NAME_KEY, value);
    }

    public static boolean isExcludeFromStyling(Widget widget){
        return (widget.getData(EXCLUDED_FROM_STYLING) == Boolean.TRUE);
    }

    public static void setExcludeFromStyling(Widget widget) {
        widget.setData(EXCLUDED_FROM_STYLING, Boolean.TRUE);
    }

    public static void markConnectionTypeColor(Widget widget) {
        if (widget != null && !widget.isDisposed()) {
            widget.setData(CSSSWTConstants.CSS_CLASS_NAME_KEY, COLORED_BY_CONNECTION_TYPE);
        }
    }

    public static Color getCurrentEditorConnectionColor(Widget widget) {
        if (!(widget instanceof Control control)) {
            return null;
        }
        try {
            for (Control c = control; c != null; c = c.getParent()) {
                Object data = c.getData(DATABASE_EDITOR_COMPOSITE_DATASOURCE);
                if (data instanceof DBPDataSourceContainer dsc) {
                    return UIUtils.getConnectionColor(dsc.getConnectionConfiguration());
                }
            }
        } catch (Exception e) {
            // Some UI issues. Probably workbench window or page wasn't yet created
        }
        return null;
    }

    public static boolean isDatabaseColored(Widget widget) {
        boolean colorByConnectionType = COLORED_BY_CONNECTION_TYPE.equals(getCSSClass(widget));
        // sometimes eclipse overrides css class of the controls, so let's check for the toolbar's css class too
        if (!colorByConnectionType && widget instanceof Composite c && c.getParent() instanceof ToolBar tb) {
            colorByConnectionType = COLORED_BY_CONNECTION_TYPE.equals(getCSSClass(tb));
        }
        return colorByConnectionType;
    }
}
