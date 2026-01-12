/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsp.utils;

import org.eclipse.lsp4j.Position;
import org.jkiss.code.NotNull;

public class LSPUtils {

    public static int positionToOffset(@NotNull String text, @NotNull Position position) {
        String[] lines = text.split("\n");
        if (lines.length <= position.getLine()) {
            throw new IllegalArgumentException("Invalid line number " + position.getLine());
        }

        int lineIndex = 0;
        int offset = 0;
        for (String line : lines) {
            if (lineIndex < position.getLine()) {
                offset += line.length() + 1;
                lineIndex++;
            } else {
                if (line.length() < position.getCharacter()) {
                    throw new IllegalArgumentException("Invalid char number " + position.getCharacter());
                }
                offset += position.getCharacter();
            }
        }

        return offset;
    }

    @NotNull
    public static Position lastTextPosition(@NotNull String text) {
        int numberOfLines = 0;
        int indexOfLastLineSeparator = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                numberOfLines++;
                indexOfLastLineSeparator = i;
            }
        }
        int startOfTheLastLine = indexOfLastLineSeparator + 1;
        if (startOfTheLastLine == text.length()) {
            return new Position(numberOfLines, 0);
        }
        return new Position(numberOfLines, text.substring(startOfTheLastLine).length());
    }
}
