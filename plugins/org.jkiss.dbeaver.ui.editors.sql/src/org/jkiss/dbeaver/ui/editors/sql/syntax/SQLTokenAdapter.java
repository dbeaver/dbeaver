/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * SQLTokenAdapter
*/
public class SQLTokenAdapter extends Token {

    private final TPToken token;

    SQLTokenAdapter(TPToken token, SQLRuleScanner scanner) {
        super(makeTextAttribute(token, scanner));
        this.token = token;
    }

    public TPToken getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "Adapter of [" + token.toString() + "]";
    }

    private static TextAttribute makeTextAttribute(TPToken token, SQLRuleScanner scanner) {
        String colorKey;
        int style;
        if (token instanceof TPTokenDefault && token.getData() instanceof SQLTokenType) {
            switch ((SQLTokenType) token.getData()) {
                case T_KEYWORD:
                case T_BLOCK_BEGIN:
                case T_BLOCK_END:
                case T_BLOCK_HEADER:
                    colorKey = SQLConstants.CONFIG_COLOR_KEYWORD;
                    style = scanner.getKeywordStyle();
                    break;
                case T_STRING:
                    colorKey = SQLConstants.CONFIG_COLOR_STRING;
                    style = scanner.getKeywordStyle();
                    break;
                case T_QUOTED:
                case T_TYPE:
                    colorKey = SQLConstants.CONFIG_COLOR_DATATYPE;
                    style = scanner.getKeywordStyle();
                    break;
                case T_NUMBER:
                    colorKey = SQLConstants.CONFIG_COLOR_NUMBER;
                    style = SWT.NORMAL;
                    break;
                case T_COMMENT:
                    colorKey = SQLConstants.CONFIG_COLOR_COMMENT;
                    style = SWT.NORMAL;
                    break;
                case T_DELIMITER:
                    colorKey = SQLConstants.CONFIG_COLOR_DELIMITER;
                    style = SWT.NORMAL;
                    break;
                case T_BLOCK_TOGGLE:
                    colorKey = SQLConstants.CONFIG_COLOR_DELIMITER;
                    style = scanner.getKeywordStyle();
                    break;
                case T_CONTROL:
                case T_SET_DELIMITER:
                    colorKey = SQLConstants.CONFIG_COLOR_COMMAND;
                    style = scanner.getKeywordStyle();
                    break;
                case T_PARAMETER:
                case T_VARIABLE:
                    colorKey = SQLConstants.CONFIG_COLOR_PARAMETER;
                    style = scanner.getKeywordStyle();
                    break;
                default:
                    colorKey = SQLConstants.CONFIG_COLOR_TEXT;
                    style = SWT.NORMAL;
                    break;
            }
        } else {
            colorKey = SQLConstants.CONFIG_COLOR_TEXT;
            style = SWT.NORMAL;
        }

        Color color = scanner.getColor(colorKey);
        if (UIStyles.isDarkHighContrastTheme()) {
            if (SQLConstants.CONFIG_COLOR_TEXT.equals(colorKey)) {
                color = UIUtils.COLOR_WHITE;
            } else {
                color = UIUtils.getInvertedColor(color);
            }
        }
        return new TextAttribute(color, null, style);
    }

}
