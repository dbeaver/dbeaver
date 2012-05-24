/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.MultiPageEditorSite;

/**
 * Content Editor Site
 */
class ContentEditorSite extends MultiPageEditorSite {

    public ContentEditorSite(ContentEditor contentEditor, IEditorPart editor)
    {
        super(contentEditor, editor);
    }

    @Override
    public IEditorActionBarContributor getActionBarContributor() {
        IEditorPart editor = getEditor();
        ContentEditor contentEditor = (ContentEditor) getMultiPageEditor();
        ContentEditor.ContentPartInfo contentPart = contentEditor.getContentEditor(editor);
        if (contentPart != null) {
            return contentPart.editorPart.getActionBarContributor();
        } else {
            return super.getActionBarContributor();
        }
    }

    @Override
    public IWorkbenchPart getPart() {
        return getMultiPageEditor();
    }

}
