/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.ui.sql;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.SQLBlockToggleToken;

class PostgreDollarQuoteRule implements IRule {

    private final IToken blockToken;

    public PostgreDollarQuoteRule() {
        blockToken = new SQLBlockToggleToken(
            new TextAttribute(UIUtils.getGlobalColor(SQLConstants.CONFIG_COLOR_DELIMITER), null, SWT.BOLD));
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c == '$') {
            int charsRead = 0;
            do {
                c = scanner.read();
                charsRead++;
                if (c == '$') {
                    if (charsRead <= 1) {
                        // Here is a trick - dollar quote without tag is a string. Quote with tag is just a block toggle.
                        // I'm afraid we can't do more ()#6608
                        break;
                    }
                    return blockToken;
                }
            } while (Character.isLetter(c) || c == '_');

            for (int i = 0; i < charsRead; i++) {
                scanner.unread();
            }
        } else {
            scanner.unread();
        }
        return Token.UNDEFINED;
    }

}
