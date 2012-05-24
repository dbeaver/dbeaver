/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;


public class SQLStringAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy
{

    /**
     * Creates a new SQL string auto indent strategy for the given document partitioning.
     * 
     * @param partitioning the document partitioning
     */
    public SQLStringAutoIndentStrategy(String partitioning)
    {
        super();
    }

    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command)
    {

    }
}
