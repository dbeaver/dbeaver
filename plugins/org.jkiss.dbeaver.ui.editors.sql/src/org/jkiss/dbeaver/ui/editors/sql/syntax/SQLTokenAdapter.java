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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

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

    private static TextAttribute makeTextAttribute(TPToken token, SQLRuleScanner scanner) {
        if (token instanceof TPTokenDefault) {
            if (token.getData() instanceof SQLTokenType) {
                switch ((SQLTokenType)token.getData()) {
                    case T_KEYWORD:
                    case T_BLOCK_BEGIN:
                    case T_BLOCK_END:
                    case T_BLOCK_HEADER:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, scanner.getKeywordStyle());
                    case T_STRING:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_STRING), null, scanner.getKeywordStyle());
                    case T_QUOTED:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_DATATYPE), null, scanner.getKeywordStyle());
                    case T_TYPE:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_DATATYPE), null, scanner.getKeywordStyle());
                    case T_NUMBER:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_NUMBER), null, SWT.NORMAL);
                    case T_COMMENT:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_COMMENT), null, SWT.NORMAL);
                    case T_DELIMITER:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_DELIMITER), null, SWT.NORMAL);
                    case T_BLOCK_TOGGLE:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_DELIMITER), null, scanner.getKeywordStyle());
                    case T_CONTROL:
                    case T_SET_DELIMITER:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, scanner.getKeywordStyle());
                    case T_PARAMETER:
                    case T_VARIABLE:
                        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_PARAMETER), null, scanner.getKeywordStyle());
                }
            }
        }
        return new TextAttribute(scanner.getColor(SQLConstants.CONFIG_COLOR_TEXT), null, SWT.NORMAL);
    }

}
