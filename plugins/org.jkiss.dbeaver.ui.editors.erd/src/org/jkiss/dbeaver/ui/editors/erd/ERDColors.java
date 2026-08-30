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
package org.jkiss.dbeaver.ui.editors.erd;

import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;

public class ERDColors {

    public static final String[] EXTRA_BORDER_COLORS = new String[] {
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_1,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_2,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_3,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_4,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_5,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_6,
            ERDUIConstants.COLOR_ERD_BORDERS_COLOR_7
    };

    public static final String[] EXTRA_HEADER_COLORS = new String[] {
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_1,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_2,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_3,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_4,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_5,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_6,
            ERDUIConstants.COLOR_ERD_HEADER_COLOR_7
    };

    public static Color getBorderColor(int index) {
        if (index >= EXTRA_BORDER_COLORS.length) {
            index = index % EXTRA_BORDER_COLORS.length;
        }
        return UIUtils.getColorRegistry().get(EXTRA_BORDER_COLORS[index]);
    }

    public static Color getHeaderColor(int index) {
        if (index >= EXTRA_HEADER_COLORS.length) {
            index = index % EXTRA_HEADER_COLORS.length;
        }
        return UIUtils.getColorRegistry().get(EXTRA_HEADER_COLORS[index]);
    }

    @NotNull
    public static Color getForeignKeyModifyRuleColor(@NotNull DBSForeignKeyModifyRule rule) {
        return getForeignKeyModifyRuleColor(rule,false);
    }

    @NotNull
    public static Color getForeignKeyModifyRuleColor(@NotNull DBSForeignKeyModifyRule rule, boolean mixWithBackground) {
        Color color = UIUtils.getColorRegistry().get(
                switch (rule) {
                    case DBSForeignKeyModifyRule.NO_ACTION   -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_NO_ACTION;
                    case DBSForeignKeyModifyRule.CASCADE     -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_CASCADE;
                    case DBSForeignKeyModifyRule.SET_NULL    -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_SET_NULL;
                    case DBSForeignKeyModifyRule.SET_DEFAULT -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_SET_DEFAULT;
                    case DBSForeignKeyModifyRule.RESTRICT    -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_RESTRICT;
                    case DBSForeignKeyModifyRule.UNKNOWN     -> ERDUIConstants.COLOR_ERD_ARROW_COLOR_UNKNOWN;
                }
        );

        if (mixWithBackground) {
            return UIStyles.mix(UIUtils.getColorRegistry().get(ERDUIConstants.COLOR_ERD_ATTR_BACKGROUND), color, 0.5f);
        } else {
            return color;
        }
    }
}
