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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.controls.SQLEditorThemeSettings;

/**
 * SQLTokenAdapter
*/
public class SQLTokenAdapter extends Token {

    private final TPToken token;

    public SQLTokenAdapter(TPToken token, SQLRuleScanner scanner) {
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
        Color color;
        int style;
        if (token instanceof TPTokenDefault && token.getData() instanceof SQLTokenType tokenType) {
            SQLEditorThemeSettings themeSettings = SQLEditorThemeSettings.instance;
            switch (tokenType) {
                case T_KEYWORD:
                case T_BLOCK_BEGIN:
                case T_BLOCK_END:
                case T_BLOCK_HEADER:
                    color = themeSettings.editorKeywordColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_STRING:
                    color = themeSettings.editorStringColor;
                    style = SWT.NORMAL;
                    break;
                case T_QUOTED:
                case T_TYPE:
                    color = themeSettings.editorDatatypeColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_FUNCTION:
                    color = themeSettings.editorFunctionColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_NUMBER:
                    color = themeSettings.editorNumberColor;
                    style = SWT.NORMAL;
                    break;
                case T_COMMENT:
                    color = themeSettings.editorCommentColor;
                    style = SWT.NORMAL;
                    break;
                case T_DELIMITER:
                    color = themeSettings.editorDelimiterColor;
                    style = SWT.NORMAL;
                    break;
                case T_BLOCK_TOGGLE:
                    color = themeSettings.editorDelimiterColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_CONTROL:
                case T_SET_DELIMITER:
                    color = themeSettings.editorCommandColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_PARAMETER:
                case T_VARIABLE:
                    color = themeSettings.editorParameterColor;
                    style = scanner.getKeywordStyle();
                    break;
                case T_TABLE:
                    color = themeSettings.editorTableColor;
                    style = SWT.NORMAL;
                    break;
                case T_TABLE_ALIAS:
                    color = themeSettings.editorTableAliasColor;
                    style = SWT.ITALIC;
                    break;
                case T_COLUMN:
                    color = themeSettings.editorColumnColor;
                    style = SWT.NORMAL;
                    break;
                case T_COLUMN_DERIVED:
                    color = themeSettings.editorColumnDerivedColor;
                    style = SWT.ITALIC;
                    break;
                case T_SCHEMA:
                    color = themeSettings.editorSchemaColor;
                    style = SWT.NORMAL;
                    break;
                case T_COMPOSITE_FIELD:
                    color = themeSettings.editorCompositeFieldColor;
                    style = SWT.NORMAL;
                    break;
                case T_SQL_VARIABLE:
                    color = themeSettings.editorSqlVariableColor;
                    style = SWT.NORMAL;
                    break;
                case T_SEMANTIC_ERROR:
                    color = themeSettings.editorSemanticErrorColor;
                    style = SWT.NORMAL;
                    break;
                default:
                    color = themeSettings.editorTextColor;
                    style = SWT.NORMAL;
                    break;
            }
        } else {
            color = SQLEditorThemeSettings.instance.editorTextColor;
            style = SWT.NORMAL;
        }

        if (UIStyles.isDarkHighContrastTheme()) {
            if (color == SQLEditorThemeSettings.instance.editorTextColor) {
                color = UIStyles.COLOR_WHITE;
            } else {
                color = UIStyles.getInvertedColor(color);
            }
        }
        return new TextAttribute(color, null, style);
    }


}
