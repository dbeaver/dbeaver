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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class SQLCharacterPairMatcher extends DefaultCharacterPairMatcher {

    private SQLEditorBase editor;

    public SQLCharacterPairMatcher(SQLEditorBase editor, char[] chars, String partitioning) {
        super(chars, partitioning);
        this.editor = editor;
    }

    public SQLCharacterPairMatcher(SQLEditorBase editor, char[] chars, String partitioning, boolean caretEitherSideOfBracket) {
        super(chars, partitioning, caretEitherSideOfBracket);
        this.editor = editor;
    }

    public SQLCharacterPairMatcher(SQLEditorBase editor, char[] chars) {
        super(chars);
        this.editor = editor;
    }

    @Override
    public boolean isMatchedChar(char ch) {
        if (editor.isBlockSelectionModeEnabled()) {
            return false;
        }
        return super.isMatchedChar(ch);
    }
}
