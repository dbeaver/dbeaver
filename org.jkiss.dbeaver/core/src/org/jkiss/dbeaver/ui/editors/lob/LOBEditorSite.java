/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor;
import org.jkiss.dbeaver.ui.editors.hex.HexEditorActionBarContributor;
import org.jkiss.dbeaver.ui.editors.hex.HexEditor;

/**
 * LOB Editor Site
 */
class LOBEditorSite extends MultiPageEditorSite {

    public LOBEditorSite(LOBEditor lobEditor, IEditorPart editor)
    {
        super(lobEditor, editor);
    }

    public IEditorActionBarContributor getActionBarContributor() {
        IEditorPart editor = getEditor();
        LOBEditor lobEditor = (LOBEditor) getMultiPageEditor();
        LOBEditor.ContentEditor contentEditor = lobEditor.getContentEditor(editor);
        if (contentEditor != null) {
            return contentEditor.actionBarContributor;
        } else {
            return super.getActionBarContributor();
        }
    }

    public IWorkbenchPart getPart() {
        return getMultiPageEditor();
    }

}
