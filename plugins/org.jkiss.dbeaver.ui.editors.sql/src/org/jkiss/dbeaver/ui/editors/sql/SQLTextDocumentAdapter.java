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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.text.TPTextDocument;
import org.jkiss.dbeaver.model.text.TPTextRegion;

/**
 * Script-to-datasource binding type
 */
public class SQLTextDocumentAdapter implements TPTextDocument {

    private final static Log log = Log.getLog(SQLTextDocumentAdapter.class);

    private final IDocument document;

    public SQLTextDocumentAdapter(IDocument document) {
        this.document = document;
    }

    @Override
    public char getChar(int offset) {
        try {
            return document.getChar(offset);
        } catch (BadLocationException e) {
            log.debug(e);
            return ' ';
        }
    }

    @Override
    public int getLength() {
        return document.getLength();
    }

    @NotNull
    @Override
    public String get() {
        return document.get();
    }

    @NotNull
    @Override
    public String get(int offset, int length) {
        try {
            return document.get(offset, length);
        } catch (BadLocationException e) {
            log.debug(e);
            return "";
        }
    }

    @Override
    public void set(@NotNull String text) {
        document.set(text);
    }

    @Override
    public void replace(int offset, int length, @NotNull String text) {
        try {
            document.replace(offset, length, text);
        } catch (BadLocationException e) {
            log.debug(e);
        }
    }

    @NotNull
    @Override
    public String[] getLegalLineDelimiters() {
        return document.getLegalLineDelimiters();
    }

    @NotNull
    @Override
    public String getLineDelimiter(int line) {
        try {
            return document.getLineDelimiter(line);
        } catch (BadLocationException e) {
            log.debug(e);
            return "\n";
        }
    }

    @Override
    public int getLineOfOffset(int offset) {
        try {
            return document.getLineOfOffset(offset);
        } catch (BadLocationException e) {
            log.debug(e);
            return 0;
        }
    }

    @Override
    public int getLineOffset(int line) {
        try {
            return document.getLineOffset(line);
        } catch (BadLocationException e) {
            log.debug(e);
            return 0;
        }
    }

    @NotNull
    @Override
    public TPTextRegion getLineInformation(int line) {
        try {
            IRegion region = document.getLineInformation(line);
            return new TPTextRegion(region.getOffset(), region.getLength());
        } catch (BadLocationException e) {
            log.debug(e);
            return new TPTextRegion(0, 0);
        }
    }

    @NotNull
    @Override
    public TPTextRegion getLineInformationOfOffset(int offset) {
        try {
            IRegion region = document.getLineInformationOfOffset(offset);
            return new TPTextRegion(region.getOffset(), region.getLength());
        } catch (BadLocationException e) {
            log.debug(e);
            return new TPTextRegion(0, 0);
        }
    }

    @Override
    public int getNumberOfLines() {
        return document.getNumberOfLines();
    }
}
