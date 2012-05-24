/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;

/**
 * SQLDocumentProvider
 */
class SQLDocumentProvider extends FileRefDocumentProvider {

    @Override
    protected IEditorInput createNewEditorInput(IFile newFile)
    {
        return new SQLEditorInput(newFile);
    }

    @Override
    protected Document createDocument(Object element) throws CoreException
    {
        Document document = super.createDocument(element);


        return document;
    }
}
