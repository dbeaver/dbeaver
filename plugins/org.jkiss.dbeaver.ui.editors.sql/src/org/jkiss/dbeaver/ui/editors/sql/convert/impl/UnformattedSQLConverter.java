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

package org.jkiss.dbeaver.ui.editors.sql.convert.impl;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLCommentToken;
import org.jkiss.dbeaver.ui.editors.sql.convert.ISQLTextConverter;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.Map;

/**
 * UnformattedSQLConverter
 */
public class UnformattedSQLConverter implements ISQLTextConverter {
    private static final Log log = Log.getLog(UnformattedSQLConverter.class);

    @NotNull
    @Override
    public String convertText(
            @NotNull SQLDialect dialect,
            @NotNull SQLSyntaxManager syntaxManager,
            @NotNull SQLRuleScanner ruleManager,
            @NotNull IDocument document,
            int startPos,
            int length,
            @NotNull Map<String, Object> options)
    {
        StringBuilder result = new StringBuilder();
        ruleManager.setRange(document, startPos, length);
        String[] singleLineComments = dialect.getSingleLineComments();
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        boolean lastWhitespace = false;
        try {
            for (;;) {
                IToken token = ruleManager.nextToken();
                if (token.isEOF()) {
                    break;
                }
                int tokenOffset = ruleManager.getTokenOffset();
                final int tokenLength = ruleManager.getTokenLength();
                if (token.isWhitespace()) {
                    if (!lastWhitespace) {
                        result.append(' ');
                    }
                    lastWhitespace = true;
                } else if (token instanceof SQLCommentToken) {
                    String comment = document.get(tokenOffset, tokenLength);
                    for (String slc : singleLineComments) {
                        if (comment.startsWith(slc)) {
                            if (multiLineComments != null) {
                                comment = multiLineComments.getFirst() + comment.substring(slc.length()) + multiLineComments.getSecond();
                            }
                            break;
                        }
                    }
                    comment = CommonUtils.compactWhiteSpaces(comment);
                    result.append(comment);
                } else {
                    lastWhitespace = false;
                    result.append(document.get(tokenOffset, tokenLength));
                }
            }
        } catch (BadLocationException e) {
            log.error("Error unformatting SQL", e);
        }
        return result.toString().trim();
    }

}
