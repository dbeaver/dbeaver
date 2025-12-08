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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.text.TPTextDocument;
import org.jkiss.dbeaver.model.text.TPTextRegion;

/**
 * SQLScriptDocument
*/
public class SQLScriptDocument implements TPTextDocument {

    private final String text;

    public SQLScriptDocument(String text) {
        this.text = text;
    }

    @Override
    public char getChar(int offset) {
        return text.charAt(offset);
    }

    @Override
    public int getLength() {
        return text.length();
    }

    @NotNull
    @Override
    public String get() {
        return text;
    }

    @NotNull
    @Override
    public String get(int offset, int length) {
        return text.substring(offset, length);
    }

    @Override
    public void set(@NotNull String text) {
        throwReadOnlyError();
    }

    @Override
    public void replace(int offset, int length, @NotNull String text) {
        throwReadOnlyError();
    }

    @NotNull
    @Override
    public String[] getLegalLineDelimiters() {
        return new String[0];
    }

    @NotNull
    @Override
    public String getLineDelimiter(int line) {
        return null;
    }

    @Override
    public int getLineOfOffset(int offset) {
        return 0;
    }

    @Override
    public int getLineOffset(int line) {
        return 0;
    }

    @NotNull
    @Override
    public TPTextRegion getLineInformation(int line) {
        return null;
    }

    @NotNull
    @Override
    public TPTextRegion getLineInformationOfOffset(int offset) {
        return null;
    }

    @Override
    public int getNumberOfLines() {
        return 0;
    }

    private static void throwReadOnlyError() {
        throw new IllegalStateException("Document is read-only");
    }


}
