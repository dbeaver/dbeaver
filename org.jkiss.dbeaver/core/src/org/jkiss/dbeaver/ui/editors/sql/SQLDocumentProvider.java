package org.jkiss.dbeaver.ui.editors.sql;

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
class SQLDocumentProvider extends FileDocumentProvider {
    protected IDocument createDocument(Object element) throws CoreException
    {
        IDocument document = super.createDocument(element);
        if (document instanceof IDocumentExtension3) {
            IDocumentExtension3 extension3 = (IDocumentExtension3) document;
            IDocumentPartitioner partitioner = new FastPartitioner( new SQLPartitionScanner(), SQLPartitionScanner.SQL_PARTITION_TYPES );
            partitioner.connect( document );
            extension3.setDocumentPartitioner( SQLPartitionScanner.SQL_PARTITIONING, partitioner );
        }

        return document;
    }
}
