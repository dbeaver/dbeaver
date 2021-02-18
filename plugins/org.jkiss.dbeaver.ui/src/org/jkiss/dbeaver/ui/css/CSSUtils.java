/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;

public class CSSUtils {

    /**
     * Set value to a widget as a CSSSWTConstants.CSS_CLASS_NAME_KEY value.
     * @param widget
     * @param value
     */
    public static void setCSSClass(Widget widget, String value){
        widget.setData(CSSSWTConstants.CSS_CLASS_NAME_KEY, value);
    }

    public static Color getCurrentEditorConnectionColor(Widget widget) {
        if (!(widget instanceof Control)) {
            return null;
        }
        try {
            for (Control c = (Control)widget; c != null; c = c.getParent()) {
                Object data = c.getData(DBStyles.DATABASE_EDITOR_COMPOSITE_DATASOURCE);
                if (data instanceof DBPDataSourceContainer) {
                    return UIUtils.getConnectionColor(
                        ((DBPDataSourceContainer) data).getConnectionConfiguration());
                }
            }
        } catch (Exception e) {
            // Some UI issues. Probably workbench window or page wasn't yet created
        }
        return null;
    }
}
