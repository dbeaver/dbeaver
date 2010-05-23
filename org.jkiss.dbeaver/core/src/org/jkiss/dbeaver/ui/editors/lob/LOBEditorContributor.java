/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.jface.action.IStatusLineManager;

/**
 * LOB Editor contributor
 */
public class LOBEditorContributor extends MultiPageEditorActionBarContributor
{
    public LOBEditorContributor()
    {
    }

    public void setActivePage(IEditorPart activeEditor)
    {
        System.out.println("PAGE=" + activeEditor);
    }

}