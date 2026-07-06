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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.jkiss.code.NotNull;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SearchQuickFilterInfo {

    // Keep this as private fields for debugging purposes
    @NotNull
    private final String text;
    private final boolean caseSensitive;
    private final boolean useRegex;
    private final boolean wholeWord;

    @NotNull
    private final Pattern pattern;
    @NotNull
    private final Predicate<String> predicate;

    public SearchQuickFilterInfo(@NotNull String text, boolean caseSensitive, boolean useRegex, boolean wholeWord) {
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
        this.predicate = this::stringMatch;
    }

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    public boolean isUseRegex() {
        return this.useRegex;
    }

    public boolean isWholeWord() {
        return this.wholeWord;
    }

    @NotNull
    public String getText() {
        return this.text;
    }

    @NotNull
    public Pattern getPattern() {
        return this.pattern;
    }

    @NotNull
    public Predicate<String> getPredicate() {
        return this.predicate;
    }

    public boolean stringMatch(@NotNull String string) {
        return this.pattern.matcher(string).find();
    }
}
