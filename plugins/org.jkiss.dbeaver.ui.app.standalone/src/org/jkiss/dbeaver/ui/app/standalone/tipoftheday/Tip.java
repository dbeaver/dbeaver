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
package org.jkiss.dbeaver.ui.app.standalone.tipoftheday;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record Tip(@NotNull String text, @NotNull List<Style> styles) {
    public Tip {
        styles = List.copyOf(styles);
    }

    public record Style(int start, int length, boolean bold, boolean italic, boolean underline, @Nullable String href) {
    }

    static final class Builder {
        private final StringBuilder text = new StringBuilder();
        private final List<Style> styles = new ArrayList<>();
        private int boldDepth;
        private int italicDepth;
        private int underlineDepth;
        @Nullable
        private String href;

        void startElement(@NotNull String name, @Nullable String elementHref) {
            switch (name.toLowerCase(Locale.ROOT)) {
                case "b" -> this.boldDepth++;
                case "i" -> this.italicDepth++;
                case "u" -> this.underlineDepth++;
                case "a" -> this.href = elementHref;
                case "br" -> this.appendLineBreak();
                default -> {
                    // ignore other tags, do nothing
                }
            }
        }

        void endElement(@NotNull String name) {
            switch (name.toLowerCase(Locale.ROOT)) {
                case "b" -> this.boldDepth--;
                case "i" -> this.italicDepth--;
                case "u" -> this.underlineDepth--;
                case "a" -> this.href = null;
                default -> {
                    // ignore other tags, do nothing
                }
            }
        }

        void appendBoldText(@NotNull String value) {
            this.boldDepth++;
            this.appendText(value);
            this.boldDepth--;
        }

        void appendText(@NotNull String value) {
            String normalized = value.replaceAll("\\p{javaWhitespace}+", " ");

            if (this.text.isEmpty() || Character.isWhitespace(this.text.charAt(this.text.length() - 1))) {
                normalized = normalized.stripLeading(); // collapse adjacent whitespaces across fragment boundaries
            }

            int start = this.text.length();
            this.text.append(normalized);
            this.addStyle(start, normalized.length());
        }

        @NotNull
        Tip build() {
            return new Tip(this.text.toString(), this.styles);
        }

        private void appendLineBreak() {
            if (!this.text.isEmpty() && this.text.charAt(text.length() - 1) != '\n') {
                this.text.append('\n');
            }
        }

        private void addStyle(int start, int length) {
            boolean isBold = this.boldDepth > 0;
            boolean isItalic = this.italicDepth > 0;
            boolean isUnderline = this.underlineDepth > 0;

            if (length > 0 && (isBold || isItalic || isUnderline || this.href != null)) {
                this.styles.add(new Style(start, length, isBold, isItalic, isUnderline, this.href));
            }
        }
    }
}
