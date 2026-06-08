/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.utils.CommonUtils;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SpreadsheetQuickFilter {

    @NotNull
    private final String text;
    private final boolean caseSensitive;
    private final boolean useRegex;
    private final boolean wholeWord;

    @NotNull
    private final Pattern pattern;
    @NotNull
    private final Predicate<String> predicate;

    public SpreadsheetQuickFilter(@NotNull String text, boolean caseSensitive, boolean useRegex, boolean wholeWord) {
        this.text = text;
        this.caseSensitive = caseSensitive;
        this.useRegex = useRegex;
        this.wholeWord = wholeWord;

        int patternFlags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        String regex = useRegex ? text : Pattern.quote(text);

        if (!text.isEmpty()) {
            if (wholeWord) {
                if (Character.isLetterOrDigit(text.charAt(0))) {
                    regex = "\\b" + regex;
                }
                if (Character.isLetterOrDigit(text.charAt(text.length() - 1))) {
                    regex = regex + "\\b";
                }
            } else {
                regex += "";
            }
        }
        this.pattern = Pattern.compile(regex, patternFlags);
        this.predicate = s -> this.pattern.matcher(s).find();
    }

    public boolean match(@NotNull ResultSetRow row, @NotNull Function<Object, String> valueFormatter) {
        for (Object value : row.getValues()) {
            String valueString = valueFormatter.apply(value);
            if (CommonUtils.isNotEmpty(valueString) && this.predicate.test(valueString)) {
                return true;
            }
        }

        return false;
    }
}
