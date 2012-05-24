/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

import java.lang.reflect.InvocationTargetException;


/**
 * BaseTextDocumentProvider
 */
public abstract class BaseTextDocumentProvider extends AbstractDocumentProvider {

    protected BaseTextDocumentProvider()
    {

    }

    protected Document createEmptyDocument()
    {
        return new Document();
    }

    @Override
    protected IAnnotationModel createAnnotationModel(Object element) throws CoreException
    {
        return new ProjectionAnnotationModel();
    }

    @Override
    protected IRunnableContext getOperationRunner(final IProgressMonitor monitor)
    {
        return new IRunnableContext() {
            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
            {
                runnable.run(monitor);
            }
        };
    }

}
