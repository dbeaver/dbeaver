/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SQLDocumentProvider
 */
class ContentTextDocumentProvider extends FileDocumentProvider implements IDocumentProviderExtension {

    static Log log = LogFactory.getLog(ContentTextDocumentProvider.class);

    protected IDocument createDocument(Object element) throws CoreException
    {
        IDocument document = super.createDocument(element);
        return document;
    }

}