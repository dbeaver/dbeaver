/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.text.FileRefDocumentProvider;


public class XMLEditor extends BaseTextEditor {

    public XMLEditor() {
        configureInsertMode(ITextEditorExtension3.SMART_INSERT, false);
        setSourceViewerConfiguration(new XMLConfiguration());
        setDocumentProvider(new FileRefDocumentProvider());
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        super.doSetInput(input);
        setupDocument();
    }

    protected void setupDocument() {
        IDocument document = getDocument();
        if (document != null) {
            IDocumentPartitioner partitioner =
                new FastPartitioner(
                    new XMLPartitionScanner(),
                    new String[]{
                        XMLPartitionScanner.XML_TAG,
                        XMLPartitionScanner.XML_COMMENT});
            partitioner.connect(document);
            ((IDocumentExtension3) document).setDocumentPartitioner(XMLPartitionScanner.XML_PARTITIONING, partitioner);
        }
    }

}
