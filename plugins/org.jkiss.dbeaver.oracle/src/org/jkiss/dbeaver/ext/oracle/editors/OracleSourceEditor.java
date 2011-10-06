/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.text.BaseTextDocumentProvider;

/**
 * Oracle source editor
 */
public class OracleSourceEditor extends SQLEditorBase {

    public OracleSourceEditor() {
        super();

        setDocumentProvider(new ObjectDocumentProvider());
    }

    public DBPDataSource getDataSource() {
        IDatabaseNodeEditorInput input = (IDatabaseNodeEditorInput) getEditorInput();
        return input.getDataSource();
    }

    private class ObjectDocumentProvider extends BaseTextDocumentProvider {

        @Override
        protected IDocument createDocument(Object element) throws CoreException {
            Document document = new Document();
            return document;
        }

        @Override
        protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {

        }

    }

}
