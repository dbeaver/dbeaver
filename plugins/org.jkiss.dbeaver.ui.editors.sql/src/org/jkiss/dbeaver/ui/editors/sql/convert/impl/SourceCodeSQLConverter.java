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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.convert.ISQLTextConverter;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * SourceCodeSQLConverter
 */
public abstract class SourceCodeSQLConverter implements ISQLTextConverter {
    private static final Log log = Log.getLog(SourceCodeSQLConverter.class);

    public static final String OPTION_KEEP_FORMATTING = "keep-formatting";
    public static final String OPTION_LINE_DELIMITER = "line-delimiter";

    private static final String DEF_LINE_DELIMITER = "\\n";

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
        try {
            String sourceText = document.get(startPos, length);
            String[] sourceLines;
            if (sourceText.contains("\n") || sourceText.contains("\r")) {
                sourceLines = sourceText.split("[\\n\\r]+");
            } else {
                sourceLines = new String[] { sourceText };
            }
            boolean keepFormatting = CommonUtils.toBoolean(options.get(OPTION_KEEP_FORMATTING));
            if (!keepFormatting) {
                for (int i = 0; i < sourceLines.length; i++) {
                    sourceLines[i] = sourceLines[i].trim();
                }
            }
            String lineDelimiter = CommonUtils.toString(options.get(OPTION_LINE_DELIMITER), DEF_LINE_DELIMITER);
            if (CommonUtils.isEmpty(lineDelimiter)) {
                lineDelimiter = " "; // Space
            }
            StringBuilder result = new StringBuilder();
            convertSourceLines(result, sourceLines, lineDelimiter, options);
            return result.toString();
        } catch (BadLocationException e) {
            log.error(e);
            return "";
        }
    }

    protected abstract void convertSourceLines(StringBuilder result, String[] sourceLines, String lineDelimiter, Map<String, Object> options);

}
