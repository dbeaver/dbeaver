/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 * Folder Editor contributor
 */
public class FolderEditorContributor extends EditorActionBarContributor
{

    @Override
    public void setActiveEditor(IEditorPart targetEditor)
    {
        EntityEditorContributor.registerSearchActions(targetEditor);
    }
}