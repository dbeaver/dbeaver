/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
