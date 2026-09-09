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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.jkiss.code.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AIChatHtmlEscaper {

    // took they from marked.js
    private static final Pattern INLINE_CODE_SPAN = Pattern.compile("(?<!`)(`+)(?!`)([^`\n]+)\\1(?!`)");

    private AIChatHtmlEscaper() {}

    @NotNull
    static String escapeText(@NotNull String text) {
        StringBuilder result = new StringBuilder(text.length() + 16);
        Matcher matcher = INLINE_CODE_SPAN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            appendEscaped(result, text, last, matcher.start());
            result.append(text, matcher.start(), matcher.end());
            last = matcher.end();
        }
        appendEscaped(result, text, last, text.length());
        return result.toString();
    }

    private static void appendEscaped(@NotNull StringBuilder result, @NotNull String text, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> result.append("&amp;");
                case '<' -> result.append("&lt;");
                default -> result.append(c);
            }
        }
    }
}
