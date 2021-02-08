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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

import java.io.File;

public class SQLEditorMatchingStrategy implements IEditorMatchingStrategy
{
    static protected final Log log = Log.getLog(SQLEditorMatchingStrategy.class);

    @Override
    public boolean matches(IEditorReference editorRef, IEditorInput input) {
        final File inputFile = EditorUtils.getLocalFileFromInput(input);
        try {
            final IEditorInput refInput = editorRef.getEditorInput();
            if (refInput != null) {
                final File refFile = EditorUtils.getLocalFileFromInput(refInput);
                if (refFile != null && refFile.equals(inputFile)) {
                    return true;
                }
            }
        } catch (PartInitException e) {
            log.debug("Error getting input from editor ref", e);
            return false;
        }
        return false;
    }
}
