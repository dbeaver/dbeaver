/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.IStorageEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * SQLDocumentProvider
 */
class LOBDocumentProvider extends FileDocumentProvider implements IDocumentProviderExtension {

    static Log log = LogFactory.getLog(LOBDocumentProvider.class);

    protected IDocument createDocument(Object element) throws CoreException
    {
        IDocument document = super.createDocument(element);
        return document;
    }

}