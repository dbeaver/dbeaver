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
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;

/**
 * SQLDocumentProvider
 */
class LOBDocumentProvider extends FileDocumentProvider {

    protected IDocument createDocument(Object element) throws CoreException
    {
        IDocument document = super.createDocument(element);
        return document;
    }

}