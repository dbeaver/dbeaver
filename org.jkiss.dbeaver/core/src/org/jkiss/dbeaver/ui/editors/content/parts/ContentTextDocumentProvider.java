/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.jkiss.dbeaver.ui.editors.FileRefDocumentProvider;

/**
 * SQLDocumentProvider
 */
class ContentTextDocumentProvider extends FileRefDocumentProvider implements IDocumentProviderExtension {

    static final Log log = LogFactory.getLog(ContentTextDocumentProvider.class);

/*
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
*/
}