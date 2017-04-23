/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.convert.ISQLTextConverter;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleManager;

import java.util.Map;

/**
 * UnformattedSQLConverter
 */
public class HTMLSQLConverter implements ISQLTextConverter {
    private static final Log log = Log.getLog(HTMLSQLConverter.class);

    @NotNull
    @Override
    public String convertText(
            @NotNull SQLDialect dialect,
            @NotNull SQLSyntaxManager syntaxManager,
            @NotNull SQLRuleManager ruleManager,
            @NotNull IDocument document,
            int startPos,
            int length,
            @NotNull Map<String, Object> options)
    {
        StringBuilder result = new StringBuilder();
        ruleManager.setRange(document, startPos, length);
        try {
            result.append("<pre>");
            for (;;) {
                IToken token = ruleManager.nextToken();
                if (token.isEOF()) {
                    break;
                }
                int tokenOffset = ruleManager.getTokenOffset();
                final int tokenLength = ruleManager.getTokenLength();
                boolean hasSpan = false;
                Object data = token.getData();
                if (data instanceof TextAttribute) {
                    result.append("<span style='");
                    TextAttribute ta = (TextAttribute) data;
                    if (ta.getBackground() != null) {
                        result.append("background-color:").append(toHex(ta.getBackground())).append(";");
                    }
                    if (ta.getForeground() != null) {
                        result.append("color:").append(toHex(ta.getForeground())).append(";");
                    }
                    if ((ta.getStyle() & SWT.BOLD) == SWT.BOLD) {
                        result.append("font-weight:bold;");
                    }
                    if ((ta.getStyle() & SWT.ITALIC) == SWT.ITALIC) {
                        result.append("font-style: italic;");
                    }

                    //ta.getStyle()
                    result.append("'>");
                    hasSpan = true;
                }
                result.append(document.get(tokenOffset, tokenLength));
                if (hasSpan) {
                    result.append("</span>");
                }
            }
            result.append("</pre>");
        } catch (BadLocationException e) {
            log.error("Error converting SQL to HTML", e);
        }
        return result.toString().trim();
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
