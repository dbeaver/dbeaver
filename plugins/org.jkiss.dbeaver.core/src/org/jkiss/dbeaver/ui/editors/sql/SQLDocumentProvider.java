/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.ui.editors.FileRefDocumentProvider;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;

/**
 * SQLDocumentProvider
 */
class SQLDocumentProvider extends FileRefDocumentProvider {

    protected IEditorInput createNewEditorInput(IFile newFile)
    {
        return new SQLEditorInput(newFile);
    }

    protected Document createDocument(Object element) throws CoreException
    {
        Document document = super.createDocument(element);

        IDocumentPartitioner partitioner = new FastPartitioner( new SQLPartitionScanner(), SQLPartitionScanner.SQL_PARTITION_TYPES );
        partitioner.connect( document );
        document.setDocumentPartitioner( SQLPartitionScanner.SQL_PARTITIONING, partitioner );

        return document;
    }
}
