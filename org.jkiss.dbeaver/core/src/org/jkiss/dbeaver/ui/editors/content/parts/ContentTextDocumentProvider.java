/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * SQLDocumentProvider
 */
class ContentTextDocumentProvider extends FileDocumentProvider implements IDocumentProviderExtension {

    static final Log log = LogFactory.getLog(ContentTextDocumentProvider.class);

    protected IDocument createDocument(Object element) throws CoreException
    {
        IDocument document = super.createDocument(element);
        return document;
    }

    @Override
    protected void handleElementContentChanged(IFileEditorInput fileEditorInput)
    {
        // Try to catch errors like OutOfMemory
        try {
            super.handleElementContentChanged(fileEditorInput);
        }
        catch (OutOfMemoryError e) {
            DBeaverUtils.showErrorDialog(
                null,
                "Out of Memory",
                "Could not load content into text editor",
                e);
        }
    }
}