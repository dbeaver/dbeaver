/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.text.StringMatcher;

/**
 *
 * Cropped and little bit changed copy of org.eclipse.ui.internal.misc.TextMatcher class
 * This TextMatcherExt can split pattern string on commas, pipes and spaces and can filter by the list of this separated words
 *
 */

public class TextMatcherExt {

    private final StringMatcher full;
    private final List<StringMatcher> parts;

    /**
     *
     * @param pattern         to match
     * @param ignoreCase      whether to do case-insensitive matching
     * @param ignoreWildCards whether to treat '?' and '*' as normal characters, not
     *                        as wildcards
     * @throws IllegalArgumentException if {@code pattern == null}
     */
    public TextMatcherExt(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
        full = new StringMatcher(pattern.trim(), ignoreCase, ignoreWildCards);
        parts = splitPattern(pattern, ignoreCase, ignoreWildCards);
    }

    private List<StringMatcher> splitPattern(String pattern,
                                             boolean ignoreCase, boolean ignoreWildCards) {
        String pat = pattern.trim();
        if (pat.isEmpty()) {
            return Collections.emptyList();
        }
        String[] subPatterns = pat.split("\\s+"); //$NON-NLS-1$
        if (subPatterns.length <= 1) {
            if (pat.contains("|")) {
                subPatterns = pat.split("\\|");
            } else if (pat.contains(",")) {
                subPatterns = pat.split(",");
            }
        }
        if (subPatterns.length <= 1) {
            return Collections.emptyList();
        }
        List<StringMatcher> matchers = new ArrayList<>();
        for (String s : subPatterns) {
            if (s == null || s.isEmpty()) {
                continue;
            }
            s = "*" + s;
            StringMatcher m = new StringMatcher(s, ignoreCase, ignoreWildCards);
            m.usePrefixMatch();
            matchers.add(m);
        }
        return matchers;
    }

    /**
     * Determines whether the given {@code text} matches the pattern.
     *
     * @param text String to match; must not be {@code null}
     * @return {@code true} if the whole {@code text} matches the pattern;
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code text == null}
     */
    public boolean match(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }
        return match(text, 0, text.length());
    }

    /**
     * Determines whether the given sub-string of {@code text} from {@code start}
     * (inclusive) to {@code end} (exclusive) matches the pattern.
     *
     * @param text  String to match in; must not be {@code null}
     * @param start start index (inclusive) within {@code text} of the sub-string to
     *              match
     * @param end   end index (exclusive) within {@code text} of the sub-string to
     *              match
     * @return {@code true} if the given slice of {@code text} matches the pattern;
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code text == null}
     */
    public boolean match(String text, int start, int end) {
        if (text == null) {
            throw new IllegalArgumentException();
        }
        if (start > end) {
            return false;
        }
        int tlen = text.length();
        start = Math.max(0, start);
        end = Math.min(end, tlen);
        if (full.match(text, start, end)) {
            return true;
        }
        if (parts.isEmpty()) {
            return false;
        }
        for (StringMatcher subMatcher : parts) {
            if (subMatcher.match(text, start, end)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return '[' + full.toString() + ',' + parts + ']';
    }
}
